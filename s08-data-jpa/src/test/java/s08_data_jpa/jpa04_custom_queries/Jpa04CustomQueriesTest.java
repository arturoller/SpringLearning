package s08_data_jpa.jpa04_custom_queries;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Testy lekcji Jpa04CustomQueries na wycinku {@code @DataJpaTest} (wystarczy: sprawdzamy tylko repozytorium i bazę).
 * Dane: dziesięć produktów z migracji db/migration/jpa04/V2. Każdy test jest wycofywany (rollback), więc UPDATE
 * i DELETE nie psują innych testów. SqlCapture (StatementInspector) zapisuje SQL wysłany do bazy.
 * Błędne zapytania (JPQL z nazwą tabeli, literówka w natywnym SQL) — Jpa04BrokenQueriesTest w pakiecie jpa04_pitfalls.
 */
@DataJpaTest(showSql = false,
        properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector="
                + "s08_data_jpa.jpa04_custom_queries.SqlCapture")
@ActiveProfiles("jpa04")
class Jpa04CustomQueriesTest {

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
        SqlCapture.clear();
    }

    private static List<String> names(List<Product> products) {
        return products.stream().map(Product::getName).toList();
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: JPQL z parametrem nazwanym (bez @Param) i pozycyjnym")
    void section1JpqlParameters() {
        assertThat(names(repository.findMoreExpensiveThan(new BigDecimal("500"))))
                .containsExactly("Laptop", "Monitor", "Krzesło biurowe");
        assertThat(SqlCapture.last()).as("JPQL p.price → kolumna price, Product → tabela product")
                .contains("from product p1_0 where p1_0.price>?");

        assertThat(names(repository.findAvailableInCategory(Category.HOME, 0)))
                .containsExactly("Kubek", "Kubek termiczny");
    }

    @Test
    @DisplayName("Sekcja 1: filtr opcjonalny (:category is null or …) — null WYŁĄCZA warunek")
    void section1OptionalFilter() {
        assertThat(names(repository.search(null, new BigDecimal("60"))))
                .as("wszystkie kategorie, cena do 60")
                .containsExactly("Mysz", "Kubek", "Kubek termiczny", "Zeszyt", "Długopis");
        assertThat(names(repository.search(Category.HOME, new BigDecimal("60"))))
                .containsExactly("Kubek", "Kubek termiczny");
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: natywny SQL — funkcja bazy H2 (REGEXP_LIKE) i wynik skalarny")
    void section2NativeQuery() {
        assertThat(names(repository.findByNameRegex("^K.*k$"))).as("zaczyna się od K, kończy na k").containsExactly("Kubek");
        assertThat(SqlCapture.last()).as("natywne zapytanie idzie do bazy bez tłumaczenia")
                .isEqualTo("SELECT * FROM product WHERE REGEXP_LIKE(name, ?) ORDER BY id");

        assertThat(repository.sumStockNative("HOME")).isEqualTo(120);
        assertThat(repository.sumStockNative("GARDEN")).isZero();
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: projekcja interfejsowa — proxy, SELECT tylko dwóch kolumn, zero wczytanych encji")
    void section3InterfaceProjection() {
        List<ProductSummary> home = repository.findByCategoryOrderByPriceAsc(Category.HOME);

        assertThat(home).extracting(ProductSummary::getName).containsExactly("Kubek", "Kubek termiczny", "Lampka");
        assertThat(home.getFirst().getPrice()).isEqualByComparingTo("24.50");
        assertThat(home.getFirst()).as("obiekt-pośrednik (proxy JDK), nie Product").isNotInstanceOf(Product.class);
        assertThat(SqlCapture.last()).startsWith("select p1_0.name,p1_0.price from product p1_0");
        assertThat(stats.getEntityLoadCount()).as("projekcja nie tworzy encji").isZero();

        assertThat(repository.findOutOfStockSummaries()).extracting(ProductSummary::getName)
                .containsExactly("Krzesło biurowe", "Lampka");
    }

    @Test
    @DisplayName("Sekcja 3: rekord DTO — z select new i BEZ new (Spring Data przepisuje zapytanie) — ten sam SQL")
    void section3DtoProjection() {
        List<ProductPrice> expected = List.of(
                new ProductPrice("Krzesło biurowe", new BigDecimal("599.00")),
                new ProductPrice("Lampka", new BigDecimal("79.00")));

        assertThat(repository.findOutOfStockPricesWithNew()).isEqualTo(expected);
        String sqlWithNew = SqlCapture.last();

        assertThat(repository.findOutOfStockPrices()).isEqualTo(expected);
        assertThat(SqlCapture.last()).isEqualTo(sqlWithNew)
                .isEqualTo("select p1_0.name,p1_0.price from product p1_0 where p1_0.stock=0 order by p1_0.name");
        assertThat(stats.getEntityLoadCount()).isZero();
    }

    @Test
    @DisplayName("Sekcja 3: projekcja dynamiczna — ta sama metoda zwraca encje, rekordy albo interfejsy")
    void section3DynamicProjection() {
        assertThat(repository.findByCategoryOrderByIdAsc(Category.OFFICE, ProductPrice.class))
                .containsExactly(new ProductPrice("Zeszyt", new BigDecimal("4.50")),
                        new ProductPrice("Długopis", new BigDecimal("2.99")),
                        new ProductPrice("Krzesło biurowe", new BigDecimal("599.00")));
        assertThat(SqlCapture.last()).startsWith("select p1_0.name,p1_0.price from");
        assertThat(stats.getEntityLoadCount()).isZero();

        assertThat(names(repository.findByCategoryOrderByIdAsc(Category.OFFICE, Product.class)))
                .containsExactly("Zeszyt", "Długopis", "Krzesło biurowe");
        assertThat(stats.getEntityLoadCount()).as("Product.class → zwykłe encje").isEqualTo(3);
    }

    @Test
    @DisplayName("Sekcja 3: PUŁAPKI projekcji — brak aliasów daje null, zła kolejność kolumn rekordu — wyjątek, @Value wczytuje encje")
    void section3ProjectionPitfalls() {
        assertThat(repository.findOutOfStockSummariesWithoutAliases())
                .as("bez „as name” gettery nie mają do czego się dopasować — po cichu null, bez wyjątku")
                .hasSize(2)
                .allSatisfy(summary -> assertThat(summary.getName()).isNull());

        assertThatThrownBy(() -> repository.findOutOfStockPricesWrongOrder())
                .as("Spring Data przepisał zapytanie na new ProductPrice(p.price, p.name) — takiego konstruktora nie ma")
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("Missing constructor for type 'ProductPrice'");

        stats.clear();
        SqlCapture.clear();
        assertThat(repository.findLabelsByCategoryOrderByIdAsc(Category.HOME)).extracting(ProductLabel::getLabel)
                .containsExactly("Kubek (24.50 zł)", "Lampka (79.00 zł)", "Kubek termiczny (59.00 zł)");
        assertThat(SqlCapture.last()).as("projekcja otwarta = pełny SELECT")
                .startsWith("select p1_0.id,p1_0.category,p1_0.created_at,p1_0.name,p1_0.price,p1_0.stock from");
        assertThat(stats.getEntityLoadCount()).as("projekcja otwarta wczytuje całe encje").isEqualTo(3);
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: @Modifying bez clear — encja w kontekście ma STARĄ cenę, nawet po ponownym findById")
    void section4StaleEntityWithoutClear() {
        Product mug = repository.findById(5L).orElseThrow(); // Kubek, 24.50 — teraz w kontekście utrwalania

        int updated = repository.changePrices(Category.HOME, new BigDecimal("2"));

        assertThat(updated).as("UPDATE zwraca liczbę zmienionych wierszy").isEqualTo(3);
        assertThat(mug.getPrice()).as("obiekt w pamięci nie wie o UPDATE").isEqualByComparingTo("24.50");
        assertThat(repository.findById(5L).orElseThrow())
                .as("findById bierze encję z kontekstu (pamięć 1. poziomu), nie z bazy").isSameAs(mug);
        tem.clear();
        assertThat(repository.findById(5L).orElseThrow().getPrice()).as("w bazie już nowa cena").isEqualByComparingTo("49.00");
    }

    @Test
    @DisplayName("Sekcja 4: @Modifying(clearAutomatically = true) — po UPDATE findById czyta z bazy świeżą cenę")
    void section4ClearAutomatically() {
        Product mug = repository.findById(5L).orElseThrow();

        repository.changePricesAndClear(Category.HOME, new BigDecimal("2"));

        assertThat(mug.getPrice()).as("stary obiekt jest teraz odłączony (detached) i nadal stary").isEqualByComparingTo("24.50");
        Product fresh = repository.findById(5L).orElseThrow();
        assertThat(fresh).isNotSameAs(mug);
        assertThat(fresh.getPrice()).isEqualByComparingTo("49.00");
    }

    @Test
    @DisplayName("Sekcja 4: Hibernate (FlushMode AUTO) sam robi flush przed UPDATE — zmiana z pamięci nie ginie przy clear")
    void section4AutoFlushBeforeBulkUpdate() {
        Product mouse = repository.findById(2L).orElseThrow();
        mouse.setStock(999); // zmiana tylko w pamięci (jeszcze bez UPDATE)
        SqlCapture.clear();

        repository.changePricesAndClear(Category.HOME, new BigDecimal("2")); // bez flushAutomatically!

        assertThat(SqlCapture.all()).as("najpierw UPDATE z dirty checking (flush), potem masowy UPDATE")
                .hasSize(2)
                .satisfies(sql -> assertThat(sql.getFirst()).startsWith("update product set category=?"))
                .satisfies(sql -> assertThat(sql.getLast()).startsWith("update product p1_0 set price="));
        assertThat(repository.findById(2L).orElseThrow().getStock()).isEqualTo(999);
    }

    @Test
    @DisplayName("Sekcja 4: masowy DELETE w @Query — jedno polecenie SQL (deleteBy… z Jpa03 robił SELECT + DELETE na encję)")
    void section4BulkDelete() {
        assertThat(repository.deleteOutOfStock()).isEqualTo(2);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(SqlCapture.all()).containsExactly("delete from product p1_0 where p1_0.stock=0");
        assertThat(stats.getEntityLoadCount()).as("żadna encja nie została wczytana").isZero();
    }

    @Test
    @DisplayName("Sekcja 4: UPDATE w @Query bez @Modifying — InvalidDataAccessApiUsageException przy WYWOŁANIU (start działa)")
    void section4MissingModifying() {
        assertThatThrownBy(() -> repository.resetStockWithoutModifying(1L))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("must be a 'select' query")
                .hasRootCauseInstanceOf(IllegalSelectQueryException.class);
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: agregaty — count → Long, avg → Double (nawet dla BigDecimal), max bez wierszy → null")
    void section5Aggregates() {
        List<CategoryStats> stats = repository.categoryStats();

        assertThat(stats).extracting(CategoryStats::category)
                .containsExactly(Category.COMPUTERS, Category.HOME, Category.OFFICE);
        assertThat(stats).extracting(CategoryStats::productCount).containsExactly(4L, 3L, 3L);
        assertThat(stats.get(1).averagePrice()).isCloseTo(54.1667, within(0.0001)); // (24.50 + 79.00 + 59.00) / 3

        assertThat(repository.maxPriceIn(Category.HOME)).isEqualByComparingTo("79.00");
        assertThat(repository.maxPriceIn(Category.GARDEN)).as("brak wierszy → null, nie 0").isNull();

        // Typy i wartości pozostałych agregatów — bezpośrednio przez EntityManager (to samo zrobiłby @Query).
        EntityManager em = tem.getEntityManager();
        assertThat(aggregate(em, "sum(p.price)", null)).isInstanceOf(BigDecimal.class);
        assertThat(aggregate(em, "sum(p.stock)", null)).as("sum pola int → Long").isInstanceOf(Long.class);
        assertThat(aggregate(em, "avg(p.price)", null)).as("avg zawsze Double").isInstanceOf(Double.class);
        assertThat(aggregate(em, "sum(p.price)", Category.GARDEN)).isNull();
        assertThat(aggregate(em, "avg(p.price)", Category.GARDEN)).isNull();
        assertThat(aggregate(em, "count(p)", Category.GARDEN)).isEqualTo(0L);
    }

    /** {@code select <agregat> from Product p [where p.category = :category]} — wynik jednej wartości. */
    private static Object aggregate(EntityManager em, String aggregate, Category category) {
        String where = category == null ? "" : " where p.category = :category";
        var query = em.createQuery("select " + aggregate + " from Product p" + where);
        if (category != null) {
            query.setParameter("category", category);
        }
        return query.getSingleResult();
    }

    @Test
    @DisplayName("Sekcja 5: SpEL #{#entityName} — Spring Data wstawia nazwę encji repozytorium")
    void section5SpelEntityName() {
        assertThat(names(repository.findOutOfStock())).containsExactly("Lampka", "Krzesło biurowe");
        assertThat(SqlCapture.last()).contains("from product p1_0 where p1_0.stock=0");
    }
}
