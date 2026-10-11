package s08_data_jpa.solutions.jpa03_query_methods;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import s08_data_jpa.jpa03_query_methods.Jpa03Application;
import s08_data_jpa.jpa03_query_methods.Product;
import s08_data_jpa.jpa03_query_methods.ProductCategory;
import s08_data_jpa.jpa03_query_methods.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Jpa03QueryMethodsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@DataJpaTest
@ContextConfiguration(classes = Jpa03Application.class)
@ActiveProfiles("jpa03")
class Jpa03QueryMethodsSolutionsTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProductRepository products;

    ProductSolutionRepository solutions;

    @BeforeEach
    void setUp() {
        solutions = new JpaRepositoryFactory(em.getEntityManager()).getRepository(ProductSolutionRepository.class);
        Product decaf = new Product("Kawa bezkofeinowa", new BigDecimal("24.00"), ProductCategory.COFFEE);
        decaf.setActive(false);
        products.saveAll(List.of(
                new Product("Kawa ziarnista", new BigDecimal("39.99"), ProductCategory.COFFEE),
                new Product("Kawa mielona", new BigDecimal("19.99"), ProductCategory.COFFEE),
                decaf,
                new Product("Herbata zielona", new BigDecimal("12.50"), ProductCategory.TEA)));
    }

    @Test
    @DisplayName("Wzorzec 1: produkty kategorii posortowane po nazwie")
    void solution1() {
        assertThat(solutions.solution1(ProductCategory.COFFEE)).extracting(Product::getName)
                .containsExactly("Kawa bezkofeinowa", "Kawa mielona", "Kawa ziarnista");
    }

    @Test
    @DisplayName("Wzorzec 2: aktywne produkty z przedziału cen, rosnąco")
    void solution2() {
        assertThat(solutions.solution2(new BigDecimal("12.50"), new BigDecimal("39.99"))).extracting(Product::getName)
                .containsExactly("Herbata zielona", "Kawa mielona", "Kawa ziarnista");
    }

    @Test
    @DisplayName("Wzorzec 3: najtańszy aktywny produkt kategorii")
    void solution3() {
        assertThat(solutions.solution3(ProductCategory.COFFEE)).map(Product::getName).contains("Kawa mielona");
        assertThat(solutions.solution3(ProductCategory.SWEETS)).isEmpty();
    }
}
