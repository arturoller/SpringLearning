package s08_data_jpa.solutions.jpa04_custom_queries;

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
import s08_data_jpa.jpa04_custom_queries.Jpa04Application;
import s08_data_jpa.jpa04_custom_queries.Product;
import s08_data_jpa.jpa04_custom_queries.ProductRepository;
import s08_data_jpa.jpa04_custom_queries.ProductSummary;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Jpa04CustomQueriesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@DataJpaTest
@ContextConfiguration(classes = Jpa04Application.class)
@ActiveProfiles("jpa04")
class Jpa04CustomQueriesSolutionsTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProductRepository products;

    ProductSolutionRepository solutions;

    @BeforeEach
    void setUp() {
        solutions = new JpaRepositoryFactory(em.getEntityManager()).getRepository(ProductSolutionRepository.class);
        products.saveAll(List.of(
                new Product("Kawa ziarnista", new BigDecimal("40.00"), "kawa", 5),
                new Product("Kawa mielona", new BigDecimal("20.00"), "kawa", 0),
                new Product("Herbata z KAWAŁKAMI owoców", new BigDecimal("15.00"), "herbata", 7)));
        products.flush();
        em.clear();
    }

    @Test
    @DisplayName("Wzorzec 1: wyszukiwanie po fragmencie nazwy bez względu na wielkość liter")
    void solution1() {
        assertThat(solutions.solution1("kawa")).extracting(Product::getName)
                .containsExactly("Herbata z KAWAŁKAMI owoców", "Kawa mielona", "Kawa ziarnista");
    }

    @Test
    @DisplayName("Wzorzec 2: rekordy ProductSummary od najdroższego")
    void solution2() {
        assertThat(solutions.solution2("kawa")).containsExactly(
                new ProductSummary("Kawa ziarnista", new BigDecimal("40.00")),
                new ProductSummary("Kawa mielona", new BigDecimal("20.00")));
    }

    @Test
    @DisplayName("Wzorzec 3: masowe zwiększenie stanu i świeże dane po zapytaniu")
    void solution3() {
        Product before = products.findAll().stream()
                .filter(product -> product.getName().equals("Kawa ziarnista"))
                .findFirst().orElseThrow();                          // encja zarządzana, stock = 5

        int changed = solutions.solution3("kawa", 10);

        assertThat(changed).isEqualTo(2);
        assertThat(products.findById(before.getId())).get().extracting(Product::getStock).isEqualTo(15);
    }
}
