package s08_data_jpa.jpa03_query_methods;

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
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jpa03QueryMethods. {@code @DataJpaTest}: każdy test wstawia 14 produktów sklepu (SampleProducts)
 * w swojej transakcji, a po teście wszystko jest wycofywane. Zapytania bez OrderBy porównujemy bez kolejności
 * ({@code containsExactlyInAnyOrder}) — baza nie obiecuje żadnej kolejności wierszy.
 */
@DataJpaTest
@ActiveProfiles("jpa03")
@ExtendWith(OutputCaptureExtension.class)
class Jpa03QueryMethodsTest {

    private static final String SELECT = "Hibernate: select p1_0.id,p1_0.category,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock "
            + "from products p1_0 ";

    @Autowired
    ProductRepository repository;

    @BeforeEach
    void insertShopProducts() {
        repository.saveAllAndFlush(SampleProducts.all());
    }

    private static String since(CapturedOutput output, int start) {
        return output.toString().substring(start);
    }

    @Test
    @DisplayName("1. findByCategory i findBySku: równość z pola encji; null w argumencie to IS NULL, a nie „pomiń warunek”")
    void equality(CapturedOutput output) {
        int start = output.length();
        assertThat(repository.findByCategory(Category.KSIAZKI)).extracting(Product::getSku)
                .containsExactlyInAnyOrder("KSI-001", "KSI-002", "KSI-003");
        assertThat(since(output, start)).contains(SELECT + "where p1_0.category=?");

        assertThat(repository.findBySku("ELE-001")).map(Product::getName).contains("Laptop Pro 14");
        assertThat(repository.findBySku("XXX-999")).isEmpty();

        start = output.length();
        assertThat(repository.findByCategory(null)).isEmpty();
        assertThat(since(output, start)).contains(SELECT + "where p1_0.category is null");
    }

    @Test
    @DisplayName("2. Operatory: LessThan, Between (włącznie), Containing+IgnoreCase (z ucieczką znaków), In, And, Or")
    void operators(CapturedOutput output) {
        int start = output.length();
        assertThat(repository.findByPriceLessThan(new BigDecimal("50"))).extracting(Product::getSku)
                .containsExactlyInAnyOrder("SPO-002", "SPO-003", "ODZ-002");
        assertThat(repository.findByPriceBetween(new BigDecimal("79.00"), new BigDecimal("129.00")))
                .extracting(Product::getSku)
                .containsExactlyInAnyOrder("KSI-001", "KSI-002", "KSI-003", "DOM-002");   // 79.00 i 129.00 też!
        assertThat(repository.findByNameContainingIgnoreCase("KAW")).extracting(Product::getSku)
                .containsExactlyInAnyOrder("SPO-001", "DOM-001");
        assertThat(repository.findByNameStartingWith("K")).extracting(Product::getSku)
                .containsExactlyInAnyOrder("SPO-001", "ODZ-001");
        assertThat(repository.findByCategoryIn(EnumSet.of(Category.DOM, Category.ODZIEZ))).hasSize(4);
        assertThat(repository.findByCategoryAndStockGreaterThan(Category.ELEKTRONIKA, 5)).extracting(Product::getSku)
                .containsExactlyInAnyOrder("ELE-001", "ELE-003");
        assertThat(repository.findByStockOrPriceGreaterThan(0, new BigDecimal("1500"))).extracting(Product::getSku)
                .containsExactlyInAnyOrder("ELE-001", "ELE-002", "SPO-003", "DOM-001");

        assertThat(since(output, start))
                .contains(SELECT + "where p1_0.price<?")
                .contains(SELECT + "where p1_0.price between ? and ?")
                .contains(SELECT + "where upper(p1_0.name) like upper(?) escape '\\'")
                .contains(SELECT + "where p1_0.name like ? escape '\\'")
                .contains(SELECT + "where p1_0.category in (?,?)")
                .contains(SELECT + "where p1_0.category=? and p1_0.stock>?")
                .contains(SELECT + "where p1_0.stock=? or p1_0.price>?");

        // % w argumencie jest szukany dosłownie (Spring Data ucieka znaki specjalne LIKE)
        assertThat(repository.findByNameContainingIgnoreCase("%")).isEmpty();
    }

    @Test
    @DisplayName("3. OrderBy sortuje w bazie, Top3/First zamieniają się na FETCH FIRST n ROWS ONLY")
    void sortingAndLimits(CapturedOutput output) {
        int start = output.length();
        assertThat(repository.findByCategoryOrderByPriceDesc(Category.ELEKTRONIKA)).extracting(Product::getSku)
                .containsExactly("ELE-001", "ELE-002", "ELE-004", "ELE-003");
        assertThat(repository.findTop3ByOrderByPriceDesc()).extracting(Product::getSku)
                .containsExactly("ELE-001", "ELE-002", "DOM-001");
        assertThat(repository.findFirstByCategoryOrderByPriceAsc(Category.SPOZYWCZE)).map(Product::getSku)
                .contains("SPO-002");

        assertThat(since(output, start))
                .contains(SELECT + "where p1_0.category=? order by p1_0.price desc")
                .contains(SELECT + "order by p1_0.price desc fetch first ? rows only")
                .contains(SELECT + "where p1_0.category=? order by p1_0.price fetch first ? rows only");
    }

    @Test
    @DisplayName("4. existsBy i countBy pytają bazę o odpowiedź, bez wczytywania encji")
    void existsAndCount(CapturedOutput output) {
        int start = output.length();
        assertThat(repository.existsBySku("ELE-001")).isTrue();
        assertThat(repository.existsBySku("XXX-999")).isFalse();
        assertThat(repository.countByCategory(Category.ELEKTRONIKA)).isEqualTo(4);
        assertThat(since(output, start))
                .contains("Hibernate: select p1_0.id from products p1_0 where p1_0.sku=? fetch first ? rows only")
                .contains("Hibernate: select count(p1_0.id) from products p1_0 where p1_0.category=?");
    }

    @Test
    @DisplayName("5. Optional przy kilku wynikach rzuca IncorrectResultSizeDataAccessException; Stream czytamy w try-with-resources")
    void resultTypes() {
        assertThatThrownBy(() -> repository.findOneByCategory(Category.DOM))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class)
                .hasMessage("Query did not return a unique result: 2 results were returned");

        try (Stream<Product> stream = repository.streamByCategory(Category.DOM)) {   // test ma transakcję
            assertThat(stream.map(Product::getSku)).containsExactlyInAnyOrder("DOM-001", "DOM-002");
        }

        // PUŁAPKA: ten sam Stream BEZ transakcji. TestTransaction.end() kończy (wycofuje) transakcję testu.
        TestTransaction.end();
        assertThatThrownBy(() -> repository.streamByCategory(Category.DOM))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageStartingWith("You're trying to execute a streaming query method without a surrounding "
                        + "transaction that keeps the connection open");
    }

    // ---------------------------------------------------------------------------------------------
    // Sekcja 6: repozytorium z literówką. Interfejs jest ZAGNIEŻDŻONY w klasie testu — domyślne skanowanie
    // repozytoriów (w aplikacji lekcji i w @DataJpaTest) pomija interfejsy zagnieżdżone, więc nie psuje innych
    // testów. Mała aplikacja TypoApp włącza je jawnie: considerNestedRepositories = true + filtr na TypoRepository.
    // ---------------------------------------------------------------------------------------------

    interface TypoRepository extends JpaRepository<Product, Long> {
        List<Product> findByNmae(String name);                   // literówka: nmae zamiast name
    }

    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Product.class)
    @EnableJpaRepositories(basePackageClasses = Product.class, considerNestedRepositories = true,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TypoRepository.class))
    static class TypoApp {
    }

    @Test
    @DisplayName("6. Literówka w nazwie metody zatrzymuje START aplikacji: No property 'nmae' found for type 'Product'")
    void typoFailsAtStartup() {
        SpringApplicationBuilder app = new SpringApplicationBuilder(TypoApp.class).web(WebApplicationType.NONE)
                .properties("spring.flyway.enabled=false", "spring.datasource.url=jdbc:h2:mem:jpa03typo");

        assertThatThrownBy(app::run)
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("No property 'nmae' found for type 'Product'; Did you mean 'name'")
                .cause().isInstanceOf(QueryCreationException.class)
                .cause().isInstanceOf(PropertyReferenceException.class)
                .hasMessage("No property 'nmae' found for type 'Product'; Did you mean 'name'");
    }
}
