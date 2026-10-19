package s08_data_jpa.jpa04_custom_queries;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jpa04CustomQueries. {@code @DataJpaTest}: przed każdym testem 14 produktów sklepu (SampleProducts),
 * po teście wycofanie transakcji. SQL sprawdzamy w przechwyconym wyjściu (show-sql).
 */
@DataJpaTest
@ActiveProfiles("jpa04")
@ExtendWith(OutputCaptureExtension.class)
class Jpa04CustomQueriesTest {

    @Autowired
    ProductRepository repository;

    @Autowired
    ProductMistakesRepository mistakes;

    @Autowired
    TestEntityManager entityManager;

    @BeforeEach
    void insertShopProducts() {
        repository.saveAllAndFlush(SampleProducts.all());
    }

    private static String since(CapturedOutput output, int start) {
        return output.toString().substring(start);
    }

    // Sekcja 1: zagnieżdżone repozytorium z błędem w JPQL (pomijane przez skanowanie lekcji — patrz Jpa03QueryMethodsTest)
    interface JpqlTypoRepository extends JpaRepository<Product, Long> {
        @Query("select p from Product p where p.cena > 100")
        List<Product> findExpensive();
    }

    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Product.class)
    @EnableJpaRepositories(basePackageClasses = Product.class, considerNestedRepositories = true,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpqlTypoRepository.class))
    static class JpqlTypoApp {
    }

    @Test
    @DisplayName("1. @Query JPQL: parametry nazwane i pozycyjne; błąd w JPQL zatrzymuje START aplikacji")
    void jpql(CapturedOutput output) {
        int start = output.length();
        assertThat(repository.findInCategoryUpTo(Category.KSIAZKI, new BigDecimal("100"))).extracting(Product::getSku)
                .containsExactly("KSI-001", "KSI-003");
        assertThat(since(output, start)).contains("Hibernate: select p1_0.id,p1_0.category,p1_0.name,p1_0.price,"
                + "p1_0.sku,p1_0.stock from products p1_0 where p1_0.category=? and p1_0.price<=? order by p1_0.price");
        assertThat(repository.findMostExpensiveFirst(Category.KSIAZKI)).extracting(Product::getSku)
                .containsExactly("KSI-002", "KSI-003", "KSI-001");
        assertThat(repository.findLowStock(5)).extracting(Product::getSku)
                .containsExactly("ELE-002", "SPO-003", "DOM-001", "KSI-003", "ELE-004");

        SpringApplicationBuilder typoApp = new SpringApplicationBuilder(JpqlTypoApp.class).web(WebApplicationType.NONE)
                .properties("spring.flyway.enabled=false", "spring.datasource.url=jdbc:h2:mem:jpa04typo");
        assertThatThrownBy(typoApp::run)
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("Query validation failed for 'select p from Product p where p.cena > 100'")
                .rootCause().hasMessage("Could not resolve attribute 'cena' of 's08_data_jpa.jpa04_custom_queries.Product'");
    }

    @Test
    @DisplayName("2. Natywne SQL: nazwy tabel i kolumn, LIMIT z H2; literówka wychodzi dopiero przy WYWOŁANIU")
    void nativeQueries(CapturedOutput output) {
        int start = output.length();
        assertThat(repository.findSoldOut()).extracting(Product::getSku).containsExactly("ELE-002", "SPO-003");
        assertThat(repository.findMostExpensiveNative("ELEKTRONIKA", 2)).extracting(Product::getSku)
                .containsExactly("ELE-001", "ELE-002");
        assertThat(since(output, start))
                .contains("Hibernate: select * from products where stock = 0 order by sku")
                .contains("Hibernate: select * from products where category = ? order by price desc limit ?");

        // kontekst wystartował mimo literówki — błąd dopiero teraz, przy wykonaniu
        assertThatThrownBy(() -> mistakes.findExpensiveWithTypo())
                .isInstanceOf(InvalidDataAccessResourceUsageException.class)
                .hasMessageContaining("Could not prepare statement")
                .hasMessageContaining("CENA");
    }

    @Test
    @DisplayName("3. Projekcja interfejsowa pobiera tylko potrzebne kolumny; agregacja z aliasami trafia do getterów")
    void interfaceProjections(CapturedOutput output) {
        int start = output.length();
        List<ProductSummary> summaries = repository.findByCategoryOrderByPriceAsc(Category.DOM);
        assertThat(summaries).extracting(ProductSummary::getName).containsExactly("Lampka biurkowa", "Ekspres do kawy");
        assertThat(Proxy.isProxyClass(summaries.getFirst().getClass())).as("obiekt-pośrednik, nie encja").isTrue();
        assertThat(since(output, start))
                .contains("Hibernate: select p1_0.name,p1_0.price from products p1_0 where p1_0.category=? order by p1_0.price");

        start = output.length();
        List<CategoryStats> stats = repository.findCategoryStats();
        assertThat(stats).extracting(CategoryStats::getCategory)
                .containsExactly(Category.DOM, Category.ELEKTRONIKA, Category.KSIAZKI, Category.ODZIEZ, Category.SPOZYWCZE);
        CategoryStats books = stats.get(2);
        assertThat(books.getProductCount()).isEqualTo(3);
        assertThat(books.getStockValue()).isEqualByComparingTo("2643.00");   // 79×15 + 129×9 + 99×3
        assertThat(since(output, start)).contains("Hibernate: select p1_0.category,count(p1_0.id),"
                + "sum((p1_0.price*p1_0.stock)) from products p1_0 group by p1_0.category order by p1_0.category");
    }

    @Test
    @DisplayName("4. Rekord DTO: z nazwy metody, z JPQL bez „new” (Spring Data przepisuje zapytanie), z „new” i z natywnego SQL")
    void recordProjections(CapturedOutput output) {
        ProductPrice lamp = new ProductPrice("Lampka biurkowa", new BigDecimal("129.00"));
        ProductPrice espresso = new ProductPrice("Ekspres do kawy", new BigDecimal("1899.00"));

        assertThat(repository.findByCategoryOrderByPriceDesc(Category.DOM)).containsExactly(espresso, lamp);

        int start = output.length();
        assertThat(repository.findPrices(Category.DOM)).containsExactly(lamp, espresso);
        // zapytanie mówiło "select p", a do bazy poszły tylko dwie kolumny konstruktora rekordu
        assertThat(since(output, start))
                .contains("Hibernate: select p1_0.name,p1_0.price from products p1_0 where p1_0.category=? order by p1_0.price");

        assertThat(repository.findPricesWithNew(Category.DOM)).containsExactly(lamp, espresso);
        assertThat(repository.findPricesNative("DOM")).containsExactly(lamp, espresso);
    }

    @Test
    @DisplayName("5. @Modifying: UPDATE/DELETE zwraca liczbę wierszy; bez @Modifying albo bez transakcji — wyjątek przy wywołaniu")
    void modifyingQueries(CapturedOutput output) {
        int start = output.length();
        assertThat(repository.changePrices(new BigDecimal("1.10"), Category.DOM)).isEqualTo(2);
        assertThat(repository.deleteSoldOut()).isEqualTo(2);
        assertThat(since(output, start))
                .contains("Hibernate: update products p1_0 set price=(p1_0.price*cast(? as numeric(10,2))) where p1_0.category=?")
                .contains("Hibernate: delete from products p1_0 where p1_0.stock=0");

        // BŁĄD 1: UPDATE bez @Modifying — Spring Data woła getResultList(), jak dla SELECT
        assertThatThrownBy(() -> mistakes.clearStockWithoutModifying(Category.DOM))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageStartingWith("Query executed via 'getResultList()' or 'getSingleResult()' must be a 'select' query");

        // BŁĄD 2: @Modifying bez transakcji. W teście jest transakcja, więc DZIAŁA (i to jest pułapka)...
        assertThat(mistakes.clearStockWithoutTransaction(Category.DOM)).isEqualTo(2);
        // ...a bez niej (jak w zwykłym serwisie bez @Transactional) — wyjątek
        TestTransaction.end();
        assertThatThrownBy(() -> mistakes.clearStockWithoutTransaction(Category.DOM))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessage("No active transaction for update or delete query")
                .hasCauseInstanceOf(jakarta.persistence.TransactionRequiredException.class);
    }

    @Test
    @DisplayName("6. Po zbiorczym UPDATE encja w kontekście jest nieaktualna, a jej flush NADPISUJE wynik UPDATE; clearAutomatically to naprawia")
    void staleEntitiesAfterBulkUpdate(CapturedOutput output) {
        // A. nieaktualny odczyt
        Product book = repository.findBySku("KSI-001").orElseThrow();      // zarządzana: cena 79.00
        repository.changePrices(new BigDecimal("2"), Category.KSIAZKI);     // w bazie już 158.00
        Product stale = repository.findById(book.getId()).orElseThrow();
        assertThat(stale).isSameAs(book);                                   // z kontekstu, bez SELECT
        assertThat(stale.getPrice()).as("nieaktualna cena z pamięci").isEqualByComparingTo("79.00");

        // B. gorzej: zmiana INNEGO pola nieaktualnej encji → flush zapisuje WSZYSTKIE kolumny, także starą cenę
        book.setStock(99);
        entityManager.flush();
        entityManager.clear();
        Product afterFlush = repository.findById(book.getId()).orElseThrow();
        assertThat(afterFlush.getStock()).isEqualTo(99);
        assertThat(afterFlush.getPrice()).as("wynik zbiorczego UPDATE przepadł").isEqualByComparingTo("79.00");

        // C. DOBRZE: flushAutomatically + clearAutomatically
        afterFlush.setStock(50);                                            // niezapisana zmiana
        int start = output.length();
        repository.changePricesAndClear(new BigDecimal("2"), Category.KSIAZKI);
        String sql = since(output, start);
        assertThat(sql.indexOf("Hibernate: update products set category=?,name=?,price=?,sku=?,stock=? where id=?"))
                .as("najpierw flush oczekującej zmiany").isNotNegative()
                .isLessThan(sql.indexOf("Hibernate: update products p1_0 set price="));
        Product fresh = repository.findById(book.getId()).orElseThrow();
        assertThat(fresh).isNotSameAs(afterFlush);                          // kontekst wyczyszczony → nowy SELECT
        assertThat(fresh.getPrice()).isEqualByComparingTo("158.00");
        assertThat(fresh.getStock()).isEqualTo(50);                        // zmiana nie przepadła
    }
}
