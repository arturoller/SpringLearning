package s08_data_jpa.jpa04_custom_queries;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Jpa04CustomQueries. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Repozytorium ćwiczeń tworzy fabryka JpaRepositoryFactory (ta sama, której używa Spring Data przy starcie).
 */
@Tag("cwiczenie")
@DataJpaTest
@ActiveProfiles("jpa04")
class Jpa04CustomQueriesExercisesTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProductRepository products;

    ProductExerciseRepository exercises;

    @BeforeEach
    void setUp() {
        exercises = new JpaRepositoryFactory(em.getEntityManager()).getRepository(ProductExerciseRepository.class);
        products.saveAll(List.of(
                new Product("Kawa ziarnista", new BigDecimal("40.00"), "kawa", 5),
                new Product("Kawa mielona", new BigDecimal("20.00"), "kawa", 0),
                new Product("Herbata z KAWAŁKAMI owoców", new BigDecimal("15.00"), "herbata", 7)));
        products.flush();
        em.clear();
    }

    @Test
    @DisplayName("Ćwiczenie 1: wyszukiwanie po fragmencie nazwy bez względu na wielkość liter")
    void exercise1() {
        assertThat(exercises.exercise1("kawa")).extracting(Product::getName)
                .containsExactly("Herbata z KAWAŁKAMI owoców", "Kawa mielona", "Kawa ziarnista");
    }

    @Test
    @DisplayName("Ćwiczenie 2: rekordy ProductSummary od najdroższego")
    void exercise2() {
        assertThat(exercises.exercise2("kawa")).containsExactly(
                new ProductSummary("Kawa ziarnista", new BigDecimal("40.00")),
                new ProductSummary("Kawa mielona", new BigDecimal("20.00")));
    }

    @Test
    @DisplayName("Ćwiczenie 3: masowe zwiększenie stanu i świeże dane po zapytaniu")
    void exercise3() {
        Product before = products.findAll().stream()
                .filter(product -> product.getName().equals("Kawa ziarnista"))
                .findFirst().orElseThrow();                          // encja zarządzana, stock = 5

        int changed = exercises.exercise3("kawa", 10);

        assertThat(changed).isEqualTo(2);
        assertThat(products.findById(before.getId())).get().extracting(Product::getStock).isEqualTo(15);
    }
}
