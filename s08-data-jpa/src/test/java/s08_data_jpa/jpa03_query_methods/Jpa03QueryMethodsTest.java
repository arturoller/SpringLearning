package s08_data_jpa.jpa03_query_methods;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jpa03QueryMethods. Dane przygotowuje {@link #givenProducts()} na początku KAŻDEGO testu (a nie
 * w {@code @BeforeEach}), bo jeden test celowo biegnie BEZ transakcji — jego dane muszą zostać posprzątane ręcznie.
 */
@DataJpaTest
@ActiveProfiles("jpa03")
class Jpa03QueryMethodsTest {

    @Autowired
    ProductRepository products;

    /** Pięć produktów: 3 kawy (jedna nieaktywna), herbata, czekolada. */
    private void givenProducts() {
        Product decaf = new Product("Kawa bezkofeinowa", new BigDecimal("24.00"), ProductCategory.COFFEE);
        decaf.setActive(false);
        products.saveAll(List.of(
                new Product("Kawa ziarnista", new BigDecimal("39.99"), ProductCategory.COFFEE),
                new Product("KAWA mielona", new BigDecimal("19.99"), ProductCategory.COFFEE),
                decaf,
                new Product("Herbata zielona", new BigDecimal("12.50"), ProductCategory.TEA),
                new Product("Czekolada gorzka", new BigDecimal("8.99"), ProductCategory.SWEETS)));
    }

    @Test
    @DisplayName("1. findByName → where name = ?; wynik w Optional (pusty, gdy brak)")
    void findByName() {
        givenProducts();

        assertThat(products.findByName("Herbata zielona")).map(Product::getPrice)
                .hasValueSatisfying(price -> assertThat(price).isEqualByComparingTo("12.50"));
        assertThat(products.findByName("Nie ma takiego")).isEmpty();
    }

    @Test
    @DisplayName("2. findByPriceLessThan → where price < ? (ostra nierówność)")
    void findByPriceLessThan() {
        givenProducts();

        assertThat(products.findByPriceLessThan(new BigDecimal("19.99"))).extracting(Product::getName)
                .containsExactlyInAnyOrder("Herbata zielona", "Czekolada gorzka");
    }

    @Test
    @DisplayName("2. Containing + IgnoreCase → like %fragment% bez względu na wielkość liter; % w danych jest escapowany")
    void findByNameContainingIgnoreCase() {
        givenProducts();

        assertThat(products.findByNameContainingIgnoreCase("kawa")).extracting(Product::getName)
                .containsExactlyInAnyOrder("Kawa ziarnista", "KAWA mielona", "Kawa bezkofeinowa");
        assertThat(products.findByNameContainingIgnoreCase("%")).as("% to zwykły znak, nie „cokolwiek”").isEmpty();
    }

    @Test
    @DisplayName("3. findByCategoryAndActiveTrue → where category = ? and active = true")
    void findByCategoryAndActiveTrue() {
        givenProducts();

        assertThat(products.findByCategoryAndActiveTrue(ProductCategory.COFFEE)).extracting(Product::getName)
                .containsExactlyInAnyOrder("Kawa ziarnista", "KAWA mielona");
    }

    @Test
    @DisplayName("4. countBy zwraca liczbę, existsBy — boolean")
    void countAndExists() {
        givenProducts();

        assertThat(products.countByCategory(ProductCategory.COFFEE)).isEqualTo(3);
        assertThat(products.existsByName("Czekolada gorzka")).isTrue();
        assertThat(products.existsByName("Lizak")).isFalse();
    }

    @Test
    @DisplayName("5. findTop3ByOrderByPriceDesc → 3 najdroższe, malejąco (order by + limit w SQL)")
    void top3ByPrice() {
        givenProducts();

        assertThat(products.findTop3ByOrderByPriceDesc()).extracting(Product::getName)
                .containsExactly("Kawa ziarnista", "Kawa bezkofeinowa", "KAWA mielona");
    }

    @Test
    @DisplayName("6. deleteBy z @Transactional usuwa i zwraca liczbę usuniętych")
    void deleteByWithTransaction() {
        givenProducts();

        long deleted = products.deleteByActiveFalse();

        assertThat(deleted).isEqualTo(1);
        assertThat(products.existsByName("Kawa bezkofeinowa")).isFalse();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)   // ten test BEZ transakcji testowej (i bez rollbacku!)
    @DisplayName("6. deleteBy BEZ transakcji → InvalidDataAccessApiUsageException (brak transakcji dla 'remove')")
    void deleteByWithoutTransactionFails() {
        try {
            givenProducts();                                  // tu zapis od razu się zatwierdza (commit)

            assertThatThrownBy(() -> products.deleteByCategory(ProductCategory.TEA))
                    .isInstanceOf(InvalidDataAccessApiUsageException.class)
                    .hasMessageContaining("No EntityManager with actual transaction available");
        } finally {
            products.deleteAllInBatch();                      // sprzątanie — bo nie ma rollbacku
        }
    }

    @Test
    @DisplayName("7. Nieistniejące pole w nazwie metody → aplikacja NIE startuje (PropertyReferenceException)")
    void wrongPropertyNameFailsAtStartup() {
        SpringApplicationBuilder app = new SpringApplicationBuilder(Jpa03Application.class, BrokenRepositoryConfig.class)
                .profiles("jpa03")
                .properties("spring.datasource.url=jdbc:h2:mem:jpa03-broken");

        assertThatThrownBy(app::run)
                .isInstanceOf(BeanCreationException.class)
                .hasRootCauseInstanceOf(PropertyReferenceException.class)
                .hasRootCauseMessage("No property 'title' found for type 'Product'");
    }

    /** Repozytorium z BŁĘDEM: Product nie ma pola title. Zagnieżdżone — automatyczne skanowanie go nie widzi. */
    interface BrokenProductRepository extends JpaRepository<Product, Long> {
        List<Product> findByTitle(String title);
    }

    /**
     * Konfiguracja rejestrująca TYLKO BrokenProductRepository. Bez {@code @Configuration} — nikt jej nie znajdzie
     * przypadkiem; podajemy ją wprost w teście sekcji 7.
     */
    @EnableJpaRepositories(basePackageClasses = Jpa03QueryMethodsTest.class, considerNestedRepositories = true,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = BrokenProductRepository.class))
    static class BrokenRepositoryConfig {
    }
}
