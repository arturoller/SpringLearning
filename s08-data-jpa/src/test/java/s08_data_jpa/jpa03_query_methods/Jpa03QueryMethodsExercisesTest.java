package s08_data_jpa.jpa03_query_methods;

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
 * Testy ĆWICZEŃ lekcji Jpa03QueryMethods. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Repozytorium ćwiczeń tworzymy RĘCZNIE: {@code new JpaRepositoryFactory(entityManager).getRepository(...)} —
 * to ta sama fabryka, której Spring Data używa przy starcie aplikacji.
 */
@Tag("cwiczenie")
@DataJpaTest
@ActiveProfiles("jpa03")
class Jpa03QueryMethodsExercisesTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProductRepository products;

    ProductExerciseRepository exercises;

    @BeforeEach
    void setUp() {
        exercises = new JpaRepositoryFactory(em.getEntityManager()).getRepository(ProductExerciseRepository.class);
        Product decaf = new Product("Kawa bezkofeinowa", new BigDecimal("24.00"), ProductCategory.COFFEE);
        decaf.setActive(false);
        products.saveAll(List.of(
                new Product("Kawa ziarnista", new BigDecimal("39.99"), ProductCategory.COFFEE),
                new Product("Kawa mielona", new BigDecimal("19.99"), ProductCategory.COFFEE),
                decaf,
                new Product("Herbata zielona", new BigDecimal("12.50"), ProductCategory.TEA)));
    }

    @Test
    @DisplayName("Ćwiczenie 1: produkty kategorii posortowane po nazwie")
    void exercise1() {
        assertThat(exercises.exercise1(ProductCategory.COFFEE)).extracting(Product::getName)
                .containsExactly("Kawa bezkofeinowa", "Kawa mielona", "Kawa ziarnista");
    }

    @Test
    @DisplayName("Ćwiczenie 2: aktywne produkty z przedziału cen, rosnąco")
    void exercise2() {
        assertThat(exercises.exercise2(new BigDecimal("12.50"), new BigDecimal("39.99"))).extracting(Product::getName)
                .containsExactly("Herbata zielona", "Kawa mielona", "Kawa ziarnista");
    }

    @Test
    @DisplayName("Ćwiczenie 3: najtańszy aktywny produkt kategorii")
    void exercise3() {
        assertThat(exercises.exercise3(ProductCategory.COFFEE)).map(Product::getName).contains("Kawa mielona");
        assertThat(exercises.exercise3(ProductCategory.SWEETS)).isEmpty();
    }
}
