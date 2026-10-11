package s08_data_jpa.jpa04_custom_queries;

import org.hibernate.query.sqm.UnknownEntityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Testy lekcji Jpa04CustomQueries. Dane: 4 produkty (2 kawy, herbata, czekolada; jedna kawa i czekolada bez towaru)
 * wstawiane w {@code @BeforeEach} — w tej samej transakcji co test, więc wycofywane po każdym teście.
 */
@DataJpaTest
@ActiveProfiles("jpa04")
class Jpa04CustomQueriesTest {

    @Autowired
    ProductRepository products;

    @Autowired
    TestEntityManager em;

    @BeforeEach
    void givenProducts() {
        products.saveAll(List.of(
                new Product("Kawa ziarnista", new BigDecimal("40.00"), "kawa", 5),
                new Product("Kawa mielona", new BigDecimal("20.00"), "kawa", 0),
                new Product("Herbata czarna", new BigDecimal("10.00"), "herbata", 7),
                new Product("Czekolada", new BigDecimal("8.00"), "slodycze", 0)));
        products.flush();
        em.clear();
    }

    @Test
    @DisplayName("1. JPQL z parametrami nazwanymi :min i :max (także bez @Param — dzięki kompilacji z -parameters)")
    void jpqlWithNamedParameters() {
        assertThat(products.findInPriceRange(new BigDecimal("10.00"), new BigDecimal("20.00")))
                .extracting(Product::getName).containsExactly("Herbata czarna", "Kawa mielona");
        assertThat(products.findAvailableInCategory("kawa"))
                .extracting(Product::getName).containsExactly("Kawa ziarnista");
    }

    @Test
    @DisplayName("1. Błąd w JPQL (literówka w nazwie encji) → aplikacja NIE startuje")
    void brokenJpqlFailsAtStartup() {
        SpringApplicationBuilder app = new SpringApplicationBuilder(Jpa04Application.class, BrokenJpqlConfig.class)
                .profiles("jpa04")
                .properties("spring.datasource.url=jdbc:h2:mem:jpa04-broken-jpql");

        assertThatThrownBy(app::run)
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("Query validation failed for 'select p from Prodcut p'")
                .hasRootCauseInstanceOf(UnknownEntityException.class);
    }

    @Test
    @DisplayName("2. Natywny SQL: nazwy tabel i kolumn bazy, wynik mapowany na encje")
    void nativeQuery() {
        assertThat(products.findOutOfStockNative())
                .extracting(Product::getName).containsExactly("Czekolada", "Kawa mielona");
    }

    @Test
    @DisplayName("2. Błąd w natywnym SQL — aplikacja startuje, wyjątek dopiero przy wywołaniu")
    void brokenNativeQueryFailsOnlyWhenCalled() {
        try (ConfigurableApplicationContext context =
                     new SpringApplicationBuilder(Jpa04Application.class, BrokenNativeConfig.class)
                             .profiles("jpa04")
                             .properties("spring.datasource.url=jdbc:h2:mem:jpa04-broken-native")
                             .run()) {
            BrokenNativeRepository broken = context.getBean(BrokenNativeRepository.class);

            assertThatThrownBy(broken::findAllFromTypo)
                    .isInstanceOf(InvalidDataAccessResourceUsageException.class)
                    .hasMessageContaining("Table \"PRODCUTS\" not found");
        }
    }

    @Test
    @DisplayName("3. PUŁAPKA @Modifying: po masowym UPDATE encja w kontekście utrwalania ma STARĄ cenę")
    void modifyingWithoutClearLeavesStaleEntities() {
        Product loaded = products.findAvailableInCategory("kawa").getFirst();   // Kawa ziarnista, 40.00, zarządzana

        int changed = products.changePrices("kawa", new BigDecimal("1.10"));

        assertThat(changed).isEqualTo(2);
        assertThat(loaded.getPrice()).isEqualByComparingTo("40.00");
        assertThat(products.findById(loaded.getId())).get().extracting(Product::getPrice)
                .as("findById bierze obiekt z kontekstu, nie z bazy").isEqualTo(new BigDecimal("40.00"));
    }

    @Test
    @DisplayName("3. @Modifying(clearAutomatically = true): kontekst wyczyszczony, findById czyta nową cenę z bazy")
    void modifyingWithClearReadsFreshData() {
        Product loaded = products.findAvailableInCategory("kawa").getFirst();

        products.changePricesAndClear("kawa", new BigDecimal("1.10"));

        assertThat(em.getEntityManager().contains(loaded)).as("stary obiekt jest odłączony").isFalse();
        assertThat(products.findById(loaded.getId())).get().extracting(Product::getPrice)
                .satisfies(price -> assertThat(price).isEqualByComparingTo("44.00"));
    }

    @Test
    @DisplayName("3. Hibernate robi flush PRZED zapytaniem @Modifying — niezapisana zmiana nie ginie przy clear")
    void pendingChangesAreFlushedBeforeBulkUpdate() {
        Product tea = products.findAvailableInCategory("herbata").getFirst();
        tea.setStock(99);                                         // zmiana tylko w pamięci (jeszcze bez UPDATE)

        products.changePricesAndClear("kawa", new BigDecimal("1.10"));

        assertThat(products.findById(tea.getId())).get().extracting(Product::getStock).isEqualTo(99);
    }

    @Test
    @DisplayName("3. @Modifying DELETE — jedno zapytanie usuwa wszystkie produkty bez towaru")
    void bulkDelete() {
        assertThat(products.deleteOutOfStock()).isEqualTo(2);
        assertThat(products.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("4. Projekcja interfejsowa: pośrednik z getterami, SQL pobiera tylko name i price")
    void interfaceProjection() {
        List<ProductNameAndPrice> rows = products.findByCategoryOrderByName("kawa");

        assertThat(rows).extracting(ProductNameAndPrice::getName, ProductNameAndPrice::getPrice)
                .containsExactly(tuple("Kawa mielona", new BigDecimal("20.00")),
                        tuple("Kawa ziarnista", new BigDecimal("40.00")));
    }

    @Test
    @DisplayName("4. Rekord (DTO) z zapytania z nazwy metody, z „select new”, z aliasów i z „select p”")
    void recordProjections() {
        List<ProductSummary> expectedKawa = List.of(
                new ProductSummary("Kawa mielona", new BigDecimal("20.00")),
                new ProductSummary("Kawa ziarnista", new BigDecimal("40.00")));

        assertThat(products.findSummariesSelectNew("kawa")).isEqualTo(expectedKawa);
        assertThat(products.findSummariesByAliases("kawa")).isEqualTo(expectedKawa);
        assertThat(products.findByStockGreaterThanOrderByName(0)).containsExactly(
                new ProductSummary("Herbata czarna", new BigDecimal("10.00")),
                new ProductSummary("Kawa ziarnista", new BigDecimal("40.00")));
        assertThat(products.findAvailableSummaries()).extracting(ProductSummary::name)
                .containsExactly("Herbata czarna", "Kawa ziarnista");
    }

    @Test
    @DisplayName("5. Natywne zapytanie z GROUP BY → projekcja interfejsowa po aliasach kolumn")
    void nativeQueryIntoInterfaceProjection() {
        assertThat(products.sumStockPerCategory())
                .extracting(CategoryStock::getCategory, CategoryStock::getTotalStock)
                .containsExactly(tuple("herbata", 7L), tuple("kawa", 5L), tuple("slodycze", 0L));
    }

    // --- Repozytoria z błędami — zagnieżdżone, więc automatyczne skanowanie ich nie widzi ----------------

    interface BrokenJpqlRepository extends JpaRepository<Product, Long> {
        @Query("select p from Prodcut p")
        List<Product> findAllFromTypo();
    }

    interface BrokenNativeRepository extends JpaRepository<Product, Long> {
        @Query(value = "select * from prodcuts", nativeQuery = true)
        List<Product> findAllFromTypo();
    }

    @EnableJpaRepositories(basePackageClasses = Jpa04CustomQueriesTest.class, considerNestedRepositories = true,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = BrokenJpqlRepository.class))
    static class BrokenJpqlConfig {
    }

    @EnableJpaRepositories(basePackageClasses = Jpa04CustomQueriesTest.class, considerNestedRepositories = true,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = BrokenNativeRepository.class))
    static class BrokenNativeConfig {
    }
}
