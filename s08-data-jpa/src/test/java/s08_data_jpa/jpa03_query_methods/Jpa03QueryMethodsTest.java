package s08_data_jpa.jpa03_query_methods;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.NonUniqueResultException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jpa03QueryMethods (sekcje 1, 2, 4 i 5) na wycinku {@code @DataJpaTest}. Dane: dziesięć produktów
 * z migracji Flyway lekcji (db/migration/jpa03/V2). Każdy test jest wycofywany, więc usunięcia nie psują innych testów.
 * Sekcja 3 (literówka w nazwie metody) — Jpa03BrokenNameTest w pakiecie jpa03_pitfalls; metody, które wymagają
 * transakcji — Jpa03TransactionRequiredTest.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("jpa03")
class Jpa03QueryMethodsTest {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private TestEntityManager tem;

    @Autowired
    private EntityManagerFactory emf;

    private Statistics stats;

    @BeforeEach
    void setUp() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
    }

    private static List<String> names(List<Product> products) {
        return products.stream().map(Product::getName).toList();
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: findByName, findByCategoryAndPriceLessThan, findByNameContainingIgnoreCase")
    void section1BasicKeywords() {
        assertThat(repository.findByName("Mysz")).get().extracting(Product::getId).isEqualTo(2L);
        assertThat(repository.findByName("mysz")).as("bez IgnoreCase wielkość liter ma znaczenie").isEmpty();

        assertThat(names(repository.findByCategoryAndPriceLessThan(Category.COMPUTERS, new BigDecimal("200"))))
                .containsExactlyInAnyOrder("Klawiatura", "Mysz");

        assertThat(names(repository.findByNameContainingIgnoreCase("KUBEK")))
                .containsExactlyInAnyOrder("Kubek", "Kubek termiczny");
        assertThat(repository.findByNameContainingIgnoreCase("%"))
                .as("znak % w parametrze jest zwykłym znakiem (Spring Data go ucieka — escape), nie „cokolwiek”")
                .isEmpty();
    }

    @Test
    @DisplayName("Sekcja 1: Between obejmuje oba końce, OrderBy…Desc sortuje malejąco")
    void section1BetweenAndOrderBy() {
        assertThat(names(repository.findByPriceBetweenOrderByPriceAscNameAsc(new BigDecimal("59.00"), new BigDecimal("79.00"))))
                .containsExactly("Kubek termiczny", "Mysz", "Lampka");

        assertThat(names(repository.findByStockGreaterThanOrderByPriceDesc(50)))
                .as("stock > 50 (nie >=), od najdroższego")
                .containsExactly("Kubek", "Zeszyt", "Długopis");
    }

    @Test
    @DisplayName("Sekcja 1: existsBy… i countBy… zwracają boolean i long")
    void section1ExistsAndCount() {
        assertThat(repository.existsByName("Mysz")).isTrue();
        assertThat(repository.existsByName("Rower")).isFalse();
        assertThat(repository.countByCategory(Category.OFFICE)).isEqualTo(3);
        assertThat(repository.countByCategory(Category.GARDEN)).isZero();
        assertThat(stats.getEntityLoadCount()).as("ani exists, ani count nie wczytują encji").isZero();
    }

    @Test
    @DisplayName("Sekcja 1: deleteBy… wczytuje pasujące encje i usuwa KAŻDĄ osobno: 1 SELECT + 2 DELETE")
    void section1DeleteBy() {
        long removed = repository.deleteByStock(0); // Lampka i Krzesło biurowe
        tem.flush();

        assertThat(removed).isEqualTo(2);
        assertThat(stats.getEntityLoadCount()).as("encje wczytane przed usunięciem").isEqualTo(2);
        assertThat(stats.getEntityDeleteCount()).isEqualTo(2);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(3);
        assertThat(repository.count()).isEqualTo(8);
    }

    @Test
    @DisplayName("Sekcja 1: findTop3By… i findFirstBy… ograniczają liczbę wyników (fetch first n rows only)")
    void section1TopAndFirst() {
        assertThat(names(repository.findTop3ByOrderByPriceDesc())).containsExactly("Laptop", "Monitor", "Krzesło biurowe");
        assertThat(repository.findFirstByCategoryOrderByPriceAsc(Category.HOME)).get()
                .extracting(Product::getName).isEqualTo("Kubek");
        assertThat(repository.findFirstByCategoryOrderByPriceAsc(Category.GARDEN)).isEmpty();
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: pojedynczy obiekt — 0 wierszy daje null, 2+ wiersze IncorrectResultSizeDataAccessException")
    void section2SingleResult() {
        assertThat(repository.findOneByCategory(Category.GARDEN)).as("brak = null (nie wyjątek!)").isNull();

        assertThatThrownBy(() -> repository.findOneByCategory(Category.OFFICE))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class)
                .hasCauseInstanceOf(NonUniqueResultException.class)
                .hasMessageContaining("3 results were returned");
    }

    @Test
    @DisplayName("Sekcja 2: Optional przy kilku wynikach też rzuca wyjątek — Optional znaczy „zero albo jeden”")
    void section2OptionalWithManyResults() {
        assertThat(repository.findOptionalBySupplier("Nikt")).isEmpty();
        assertThatThrownBy(() -> repository.findOptionalBySupplier("Nowak"))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class)
                .hasMessageContaining("4 results were returned");
    }

    @Test
    @DisplayName("Sekcja 2: Stream w transakcji (tu: transakcja testu) i zamknięty przez try-with-resources")
    void section2Stream() {
        try (Stream<Product> products = repository.streamByCategory(Category.HOME)) {
            assertThat(products.map(Product::getName).sorted().toList())
                    .containsExactly("Kubek", "Kubek termiczny", "Lampka");
        }
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: null jako parametr daje warunek IS NULL — a nie „pomiń ten warunek”")
    void section4NullParameter() {
        assertThat(names(repository.findBySupplierOrderByIdAsc(null)))
                .as("produkty BEZ dostawcy").containsExactly("Kubek", "Lampka", "Zeszyt");
        assertThat(repository.findByCategory(null)).as("category IS NULL — kolumna NOT NULL, więc nic").isEmpty();
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: Query by Example — próbka encji; PUŁAPKA: pole typu prostego (int stock = 0) też jest warunkiem")
    void section5QueryByExample() {
        Product probe = new Product(null, Category.HOME, null, 0, null, null); // probe = próbka

        assertThat(names(repository.findAll(Example.of(probe), Sort.by("id"))))
                .as("stock = 0 wszedł do warunku").containsExactly("Lampka");

        ExampleMatcher ignoreStock = ExampleMatcher.matching().withIgnorePaths("stock");
        assertThat(names(repository.findAll(Example.of(probe, ignoreStock), Sort.by("id"))))
                .containsExactly("Kubek", "Lampka", "Kubek termiczny");
    }
}
