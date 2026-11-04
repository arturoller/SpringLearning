package s07_data_jdbc.jdbc05_repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc05Repository (sekcje 1, 2, 4 i 5) na wycinku @JdbcTest. Wycinek NIE zawiera beanów
 * z adnotacją @Repository, więc repozytorium dokładamy przez @Import (dlaczego — Jdbc05SliceWithoutImportTest).
 * Schemat i dane pochodzą z migracji Flyway lekcji (profil "jdbc05"), a każdy test wycofuje swoje zmiany.
 */
@JdbcTest
@ActiveProfiles("jdbc05")
@Import(JdbcProductRepository.class)
class Jdbc05RepositoryTest {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private JdbcClient jdbc;

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: findById zwraca produkt albo Optional.empty(), findAll i findByCategory są posortowane")
    void section1Reading() {
        assertThat(repository.findById(2)).get().extracting(Product::name).isEqualTo("Mysz");
        assertThat(repository.findById(999)).isEmpty();
        assertThat(repository.findAll()).extracting(Product::name)
                .containsExactly("Klawiatura", "Mysz", "Monitor", "Kubek", "Lampka");
        assertThat(repository.findByCategory("dom")).extracting(Product::name).containsExactly("Kubek", "Lampka");
        assertThat(repository.findByCategory("ogród")).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 1: save bez numeru robi INSERT i zwraca produkt z numerem nadanym przez bazę")
    void section1Insert() {
        Product saved = repository.save(Product.create("Zeszyt", "dom", new BigDecimal("4.50"), 40));

        assertThat(saved.id()).isGreaterThan(5);
        assertThat(saved.version()).isZero();
        assertThat(repository.findById(saved.id())).contains(saved);
        assertThat(repository.findAll()).hasSize(6);
    }

    @Test
    @DisplayName("Sekcja 1: save z numerem robi UPDATE, podnosi wersję i nie dodaje wierszy")
    void section1Update() {
        Product mouse = repository.findById(2).orElseThrow();

        Product saved = repository.save(mouse.withPrice(new BigDecimal("49.00")).withStock(25));

        assertThat(saved.version()).isEqualTo(mouse.version() + 1);
        Product reloaded = repository.findById(2).orElseThrow();
        assertThat(reloaded.price()).isEqualByComparingTo("49.00");
        assertThat(reloaded.stock()).isEqualTo(25);
        assertThat(repository.findAll()).hasSize(5);
    }

    @Test
    @DisplayName("Sekcja 1: deleteById zwraca true, gdy usunięto wiersz, i false dla nieistniejącego numeru")
    void section1Delete() {
        assertThat(repository.deleteById(4)).isTrue();
        assertThat(repository.deleteById(4)).isFalse();
        assertThat(repository.findById(4)).isEmpty();
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: złamanie UNIQUE daje DuplicateKeyException (podklasę DataIntegrityViolationException)")
    void section2UniqueViolation() {
        Product duplicate = Product.create("Mysz", "dom", new BigDecimal("10"), 1);

        assertThatThrownBy(() -> repository.save(duplicate))
                .isInstanceOf(DuplicateKeyException.class)
                .isInstanceOf(DataIntegrityViolationException.class)
                .isInstanceOf(DataAccessException.class)
                .hasRootCauseInstanceOf(SQLException.class)
                .hasMessageContaining("UK_PRODUCT_NAME");
    }

    @Test
    @DisplayName("Sekcja 2: NOT NULL, klucz obcy i CHECK dają DataIntegrityViolationException, ale NIE DuplicateKeyException")
    void section2OtherIntegrityViolations() {
        assertThatThrownBy(() -> repository.save(Product.create(null, "dom", new BigDecimal("10"), 1)))
                .as("NOT NULL")
                .isExactlyInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("NULL not allowed");
        assertThatThrownBy(() -> repository.save(Product.create("Grabie", "ogród", new BigDecimal("10"), 1)))
                .as("FOREIGN KEY: kategoria 'ogród' nie istnieje")
                .isExactlyInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("FK_PRODUCT_CATEGORY");
        assertThatThrownBy(() -> repository.save(Product.create("Grabie", "dom", new BigDecimal("10"), -1)))
                .as("CHECK (stock >= 0)")
                .isExactlyInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("CK_PRODUCT_STOCK");
    }

    @Test
    @DisplayName("Sekcja 2: wyjątki bazy nie są sprawdzane (to RuntimeException), więc nie trzeba ich deklarować ani łapać")
    void section2UncheckedExceptions() {
        assertThat(RuntimeException.class.isAssignableFrom(DataAccessException.class)).isTrue();
        assertThat(Exception.class.isAssignableFrom(SQLException.class)).as("SQLException jest sprawdzany").isTrue();
        assertThat(RuntimeException.class.isAssignableFrom(SQLException.class)).isFalse();
        assertThat(DataAccessException.class.isAssignableFrom(DuplicateKeyException.class)).isTrue();
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: wycinek @JdbcTest uruchomił migracje Flyway, więc repozytorium działa na prawdziwym schemacie")
    void section4SliceRunsMigrations() {
        assertThat(jdbc.sql("SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"type\" = 'SQL'")
                .query(Integer.class).single()).isEqualTo(2);
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: zapis na starej wersji (ktoś był szybszy) rzuca OptimisticLockingFailureException")
    void section5OptimisticLocking() {
        Product ofAnna = repository.findById(1).orElseThrow();
        Product ofJan = repository.findById(1).orElseThrow(); // dwie osoby wczytały ten sam produkt (wersja 0)

        repository.save(ofAnna.withStock(11)); // Anna zapisuje pierwsza → wersja 1

        assertThatThrownBy(() -> repository.save(ofJan.withStock(99)))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .isInstanceOf(ConcurrencyFailureException.class)
                .hasMessageContaining("nasza wersja: 0");
        Product current = repository.findById(1).orElseThrow();
        assertThat(current.stock()).as("zmiana Jana nie nadpisała zmiany Anny").isEqualTo(11);
        assertThat(current.version()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sekcja 5: po odświeżeniu wersji (ponowny odczyt) zapis się udaje")
    void section5RetryAfterReload() {
        Product stale = repository.findById(1).orElseThrow();
        repository.save(stale.withStock(11));

        Product fresh = repository.findById(1).orElseThrow();
        Product saved = repository.save(fresh.withStock(fresh.stock() + 1));

        assertThat(saved.stock()).isEqualTo(12);
        assertThat(saved.version()).isEqualTo(2);
    }

    @Test
    @DisplayName("Sekcja 5: UPDATE dla usuniętego produktu też daje konflikt (0 zmienionych wierszy)")
    void section5UpdateOfDeletedRow() {
        Product product = repository.findById(1).orElseThrow();
        repository.deleteById(1);

        assertThatThrownBy(() -> repository.save(product.withStock(1)))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
