package s08_data_jpa.solutions.jpa02_repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import s08_data_jpa.jpa02_repositories.Category;
import s08_data_jpa.jpa02_repositories.Jpa02Application;
import s08_data_jpa.jpa02_repositories.Product;
import s08_data_jpa.jpa02_repositories.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprawdzenia ćwiczeń 1 i 2 z Jpa02RepositoriesExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")}.
 * Rozwiązania używają ProductRepository z lekcji, więc {@code @ContextConfiguration} wskazuje klasę startową LEKCJI
 * (bez tego @DataJpaTest znalazłby Jpa02SolutionsApplication z tego pakietu, która nie zna repozytoriów lekcji).
 */
@DataJpaTest
@ContextConfiguration(classes = Jpa02Application.class)
@ActiveProfiles("jpa02")
@Tag("wzorzec")
class Jpa02RepositoriesSolutionsTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    @DisplayName("Wzorzec 1: dostawa zwiększa stan magazynowy; nieznane id → IllegalArgumentException")
    void solution1() {
        Long id = entityManager.persistAndGetId(
                new Product("ELE-001", "Laptop Pro 14", Category.ELEKTRONIKA, new BigDecimal("5499.99"), 7), Long.class);
        entityManager.flush();
        entityManager.clear();

        assertThat(Jpa02RepositoriesSolutions.solution1(productRepository, id, 5)).isEqualTo(12);
        entityManager.flush();
        entityManager.clear();
        assertThat(entityManager.find(Product.class, id).getStock()).isEqualTo(12);

        assertThatThrownBy(() -> Jpa02RepositoriesSolutions.solution1(productRepository, -1L, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Brak produktu o id -1");
    }

    @Test
    @DisplayName("Wzorzec 2: usuwa produkty z zerowym stanem i zwraca ich liczbę")
    void solution2() {
        productRepository.saveAll(List.of(
                new Product("ELE-001", "Laptop Pro 14", Category.ELEKTRONIKA, new BigDecimal("5499.99"), 7),
                new Product("ELE-002", "Smartfon X", Category.ELEKTRONIKA, new BigDecimal("2999.00"), 0),
                new Product("SPO-003", "Oliwa z oliwek", Category.SPOZYWCZE, new BigDecimal("42.00"), 0),
                new Product("KSI-001", "Czysty kod", Category.KSIAZKI, new BigDecimal("79.00"), 15)));
        entityManager.flush();
        entityManager.clear();

        assertThat(Jpa02RepositoriesSolutions.solution2(productRepository)).isEqualTo(2);
        entityManager.flush();
        entityManager.clear();
        assertThat(productRepository.findAll()).extracting(Product::getSku)
                .containsExactlyInAnyOrder("ELE-001", "KSI-001");
    }
}
