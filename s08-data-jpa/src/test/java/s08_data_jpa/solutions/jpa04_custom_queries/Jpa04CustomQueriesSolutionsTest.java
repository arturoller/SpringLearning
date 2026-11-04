package s08_data_jpa.solutions.jpa04_custom_queries;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import s08_data_jpa.jpa04_custom_queries.Category;
import s08_data_jpa.jpa04_custom_queries.Product;
import s08_data_jpa.jpa04_custom_queries.ProductPrice;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Jpa04CustomQueriesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@DataJpaTest(showSql = false)
@ActiveProfiles("jpa04")
class Jpa04CustomQueriesSolutionsTest {

    @Autowired
    private SolutionProductRepository repository;

    private static List<String> names(List<Product> products) {
        return products.stream().map(Product::getName).toList();
    }

    @Test
    @DisplayName("Wzorzec 1: produkty z fragmentem nazwy, bez względu na wielkość liter")
    void solution1() {
        assertThat(names(repository.searchByNameFragment("kub"))).containsExactly("Kubek", "Kubek termiczny");
        assertThat(names(repository.searchByNameFragment("MONI"))).containsExactly("Monitor");
        assertThat(repository.searchByNameFragment("rower")).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 2: rekordy ProductPrice kategorii od najtańszego")
    void solution2() {
        assertThat(repository.pricesInCategory(Category.HOME)).containsExactly(
                new ProductPrice("Kubek", new BigDecimal("24.50")),
                new ProductPrice("Kubek termiczny", new BigDecimal("59.00")),
                new ProductPrice("Lampka", new BigDecimal("79.00")));
        assertThat(repository.pricesInCategory(Category.GARDEN)).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 3: wartość magazynu kategorii, 0 dla kategorii bez produktów")
    void solution3() {
        assertThat(repository.stockValue(Category.HOME)).isEqualByComparingTo("3630.00"); // 24.50·100 + 79·0 + 59·20
        assertThat(repository.stockValue(Category.GARDEN)).isNotNull().isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Wzorzec 4: masowe uzupełnienie stanów zerowych, findById widzi nowy stan")
    void solution4() {
        Product lamp = repository.findById(6L).orElseThrow(); // Lampka, stan 0 — teraz w kontekście utrwalania
        assertThat(lamp.getStock()).isZero();

        assertThat(repository.restockEmpty(15)).isEqualTo(2); // Lampka i Krzesło biurowe

        assertThat(repository.findById(6L).orElseThrow().getStock()).as("findById musi czytać z bazy").isEqualTo(15);
        assertThat(repository.findById(10L).orElseThrow().getStock()).isEqualTo(15);
    }
}
