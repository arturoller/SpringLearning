package s08_data_jpa.jpa05_paging_sorting;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Jpa05PagingSorting (ExerciseProductRepository). @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@DataJpaTest(showSql = false)
@ActiveProfiles("jpa05")
class Jpa05PagingSortingExercisesTest {

    @Autowired
    private ExerciseProductRepository repository;

    @Autowired
    private EntityManagerFactory emf;

    private static List<String> names(Iterable<Product> products) {
        List<String> names = new java.util.ArrayList<>();
        products.forEach(product -> names.add(product.getName()));
        return names;
    }

    @Test
    @DisplayName("Ćwiczenie 1: strona kategorii posortowana po nazwie, z liczbą wszystkich wyników")
    void exercise1() {
        Page<Product> page = repository.categoryPageByName(Category.COMPUTERS, 1, 2);
        assertThat(names(page)).containsExactly("Monitor", "Mysz");
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("Ćwiczenie 2: Slice dostępnych produktów od najtańszego, remisy po id")
    void exercise2() {
        Slice<Product> second = repository.availableCheapestFirst(1, 4);
        assertThat(names(second)).containsExactly("Mysz", "Kubek termiczny", "Segregator", "Klawiatura");
        assertThat(second.hasNext()).isTrue();

        Slice<Product> third = repository.availableCheapestFirst(2, 4);
        assertThat(names(third)).containsExactly("Monitor", "Laptop");
        assertThat(third.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 3: n najdroższych produktów kategorii przez Limit")
    void exercise3() {
        assertThat(names(repository.mostExpensive(Category.OFFICE, 2))).containsExactly("Krzesło biurowe", "Segregator");
        assertThat(names(repository.mostExpensive(Category.HOME, 5))).containsExactly("Lampka", "Kubek termiczny", "Kubek");
    }

    @Test
    @DisplayName("Ćwiczenie 4: wszystkie nazwy alfabetycznie, pobierane oknami po 5 (keyset)")
    void exercise4() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        assertThat(repository.allNamesInWindowsOfFive()).containsExactly(
                "Długopis", "Klawiatura", "Krzesło biurowe", "Kubek", "Kubek termiczny", "Lampka",
                "Laptop", "Monitor", "Mysz", "Podkładka pod mysz", "Segregator", "Zeszyt");
        assertThat(stats.getPrepareStatementCount()).as("12 produktów oknami po 5 = 3 zapytania").isEqualTo(3);
    }
}
