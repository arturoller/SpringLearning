package s07_data_jdbc.jdbc05_repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc05Repository (test wycinkowy {@code @JdbcTest}) — „wynik” sekcji 1–4.
 *
 * <p>Wybór wycinka: repozytorium potrzebuje tylko bazy i JdbcClient, więc {@code @JdbcTest} jest najwęższym
 * wycinkiem, który wystarcza (szybszy niż {@code @SpringBootTest}). {@code @Import} dołącza repozytorium, bo wycinek
 * nie skanuje komponentów; {@code @ActiveProfiles} włącza ustawienia lekcji (katalog migracji Flyway).</p>
 */
@JdbcTest
@Import(ProductRepository.class)
@ActiveProfiles("jdbc05")
class Jdbc05RepositoryJdbcTest {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("1. Obieg: zapis z nadanym id, odczyt, zmiana ceny, usunięcie")
    void section1() {
        assertThat(repository.count()).isEqualTo(14);   // dane z migracji V2

        Product saved = repository.save(new NewProduct("DOM-003", "Odkurzacz", "DOM", new BigDecimal("599.00"), 3));
        assertThat(saved.id()).isGreaterThan(14L);        // id nadała baza (numery 1-14 zajęły produkty z migracji)
        assertThat(repository.findBySku("DOM-003")).contains(saved);
        assertThat(repository.findByCategory("DOM")).extracting(Product::sku)
                .containsExactly("DOM-001", "DOM-002", "DOM-003");

        assertThat(repository.updatePrice("DOM-003", new BigDecimal("549.00"))).isTrue();
        assertThat(repository.findBySku("DOM-003")).get().extracting(Product::price).isEqualTo(new BigDecimal("549.00"));

        assertThat(repository.deleteBySku("DOM-003")).isTrue();
        assertThat(repository.findBySku("DOM-003")).isEmpty();
        assertThat(repository.count()).isEqualTo(14);
    }

    @Test
    @DisplayName("2. Brak produktu to pusty Optional albo false — nie wyjątek i nie null")
    void section2() {
        assertThat(repository.findBySku("NIE-MA")).isEmpty();
        assertThat(repository.updatePrice("NIE-MA", BigDecimal.TEN)).isFalse();
        assertThat(repository.deleteBySku("NIE-MA")).isFalse();
        assertThat(repository.findByCategory("NIE-MA")).isEmpty();
    }

    @Test
    @DisplayName("3. Duplikat SKU: DuplicateKeyException zamieniony na wyjątek domenowy; inne naruszenia to nie duplikat")
    void section3() {
        NewProduct duplicate = new NewProduct("ELE-001", "Inny laptop", "ELEKTRONIKA", new BigDecimal("1.00"), 1);

        assertThatThrownBy(() -> repository.save(duplicate))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessage("Produkt o SKU ELE-001 już istnieje")
                .hasCauseInstanceOf(DuplicateKeyException.class);   // oryginalny wyjątek zachowany jako przyczyna
        assertThat(repository.count()).isEqualTo(14);                // nic nie zostało dopisane

        // pozostałe ograniczenia: DataIntegrityViolationException, ale to NIE duplikat
        JdbcClient jdbc = JdbcClient.create(dataSource);
        assertThatThrownBy(() -> repository.save(new NewProduct("TST-001", "Ujemna cena", "DOM", new BigDecimal("-1.00"), 1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(DuplicateKeyException.class)
                .isNotInstanceOf(DuplicateSkuException.class);       // CHECK (price >= 0)
        assertThatThrownBy(() -> repository.save(new NewProduct("TST-002", null, "DOM", BigDecimal.ONE, 1)))
                .isInstanceOf(DataIntegrityViolationException.class); // NOT NULL na name
        assertThat(jdbc.sql("SELECT COUNT(*) FROM product").query(Integer.class).single()).isEqualTo(14);
    }

    @Test
    @DisplayName("4. Wycinek @JdbcTest: jest JdbcClient i Flyway, nie ma skanowanych komponentów, baza jest podmieniona")
    void section4() throws SQLException {
        assertThat(context.getBeanNamesForType(JdbcClient.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(JdbcTemplate.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(Flyway.class)).hasSize(1);   // migracje wykonane

        // ProductRepository jest, bo dołączył go @Import; inne komponenty lekcji (np. klasa startowa) nie są skanowane
        assertThat(context.getBeanNamesForType(ProductRepository.class)).hasSize(1);

        // baza podmieniona na wbudowaną o losowej nazwie, a nie jdbc:h2:mem:jdbc05 z application-jdbc05.properties
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:").doesNotContain("jdbc05");
        }
        // w wycinku repozytorium to zwykły obiekt (bez proxy tłumaczącego wyjątki) — zob. test pełnego kontekstu
        assertThat(AopUtils.isAopProxy(repository)).isFalse();
    }

    @Test
    @DisplayName("Test działa w transakcji, a jej wycofanie cofa zapisy")
    void rollbackUndoesWrites() {
        assertThat(TestTransaction.isActive()).isTrue();             // @JdbcTest otwiera transakcję na czas testu
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue(); // i domyślnie ją wycofuje

        repository.save(new NewProduct("TST-100", "Tymczasowy", "DOM", BigDecimal.ONE, 1));
        assertThat(repository.count()).isEqualTo(15);

        TestTransaction.end();                                       // wycofanie teraz, w środku testu
        TestTransaction.start();
        assertThat(repository.count()).isEqualTo(14);                // zapis cofnięty
    }
}
