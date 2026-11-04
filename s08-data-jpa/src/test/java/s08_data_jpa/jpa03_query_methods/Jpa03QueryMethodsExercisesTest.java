package s08_data_jpa.jpa03_query_methods;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Jpa03QueryMethods (ExerciseProductRepository). @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@DataJpaTest(showSql = false)
@ActiveProfiles("jpa03")
class Jpa03QueryMethodsExercisesTest {

    @Autowired
    private ExerciseProductRepository repository;

    private static List<String> names(List<Product> products) {
        return products.stream().map(Product::getName).toList();
    }

    @Test
    @DisplayName("Ćwiczenie 1: produkty dostawcy posortowane po nazwie")
    void exercise1() {
        assertThat(names(repository.bySupplierSortedByName("Kowalski"))).containsExactly("Długopis", "Laptop", "Monitor");
        assertThat(repository.bySupplierSortedByName("Nikt")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 2: dostępne produkty kategorii od najtańszego")
    void exercise2() {
        assertThat(names(repository.availableInCategoryCheapestFirst(Category.HOME))).containsExactly("Kubek", "Kubek termiczny");
        assertThat(names(repository.availableInCategoryCheapestFirst(Category.OFFICE))).containsExactly("Długopis", "Zeszyt");
    }

    @Test
    @DisplayName("Ćwiczenie 3: liczba nazw zaczynających się od tekstu, bez względu na wielkość liter")
    void exercise3() {
        assertThat(repository.countNameStartingWithIgnoreCase("kubek")).isEqualTo(2);
        assertThat(repository.countNameStartingWithIgnoreCase("K")).isEqualTo(4);
        assertThat(repository.countNameStartingWithIgnoreCase("termiczny")).as("StartingWith, nie Containing").isZero();
    }

    @Test
    @DisplayName("Ćwiczenie 4: dwa najtańsze produkty bez dostawcy")
    void exercise4() {
        assertThat(names(repository.twoCheapestWithoutSupplier())).containsExactly("Zeszyt", "Kubek");
    }
}
