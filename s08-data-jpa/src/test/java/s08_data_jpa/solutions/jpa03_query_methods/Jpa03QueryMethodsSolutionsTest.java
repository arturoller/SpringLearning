package s08_data_jpa.solutions.jpa03_query_methods;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import s08_data_jpa.jpa03_query_methods.Category;
import s08_data_jpa.jpa03_query_methods.Product;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Jpa03QueryMethodsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@DataJpaTest(showSql = false)
@ActiveProfiles("jpa03")
class Jpa03QueryMethodsSolutionsTest {

    @Autowired
    private SolutionProductRepository repository;

    private static List<String> names(List<Product> products) {
        return products.stream().map(Product::getName).toList();
    }

    @Test
    @DisplayName("Wzorzec 1: produkty dostawcy posortowane po nazwie")
    void solution1() {
        assertThat(names(repository.bySupplierSortedByName("Kowalski"))).containsExactly("Długopis", "Laptop", "Monitor");
        assertThat(repository.bySupplierSortedByName("Nikt")).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 2: dostępne produkty kategorii od najtańszego")
    void solution2() {
        assertThat(names(repository.availableInCategoryCheapestFirst(Category.HOME))).containsExactly("Kubek", "Kubek termiczny");
        assertThat(names(repository.availableInCategoryCheapestFirst(Category.OFFICE))).containsExactly("Długopis", "Zeszyt");
    }

    @Test
    @DisplayName("Wzorzec 3: liczba nazw zaczynających się od tekstu, bez względu na wielkość liter")
    void solution3() {
        assertThat(repository.countNameStartingWithIgnoreCase("kubek")).isEqualTo(2);
        assertThat(repository.countNameStartingWithIgnoreCase("K")).isEqualTo(4);
        assertThat(repository.countNameStartingWithIgnoreCase("termiczny")).as("StartingWith, nie Containing").isZero();
    }

    @Test
    @DisplayName("Wzorzec 4: dwa najtańsze produkty bez dostawcy")
    void solution4() {
        assertThat(names(repository.twoCheapestWithoutSupplier())).containsExactly("Zeszyt", "Kubek");
    }
}
