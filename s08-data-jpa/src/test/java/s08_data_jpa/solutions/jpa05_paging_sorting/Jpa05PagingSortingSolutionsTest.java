package s08_data_jpa.solutions.jpa05_paging_sorting;

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
import s08_data_jpa.jpa05_paging_sorting.Category;
import s08_data_jpa.jpa05_paging_sorting.Product;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Jpa05PagingSortingExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@DataJpaTest(showSql = false)
@ActiveProfiles("jpa05")
class Jpa05PagingSortingSolutionsTest {

    @Autowired
    private SolutionProductRepository repository;

    @Autowired
    private EntityManagerFactory emf;

    private static List<String> names(Iterable<Product> products) {
        List<String> names = new java.util.ArrayList<>();
        products.forEach(product -> names.add(product.getName()));
        return names;
    }

    @Test
    @DisplayName("Wzorzec 1: strona kategorii posortowana po nazwie, z liczbą wszystkich wyników")
    void solution1() {
        Page<Product> page = repository.categoryPageByName(Category.COMPUTERS, 1, 2);
        assertThat(names(page)).containsExactly("Monitor", "Mysz");
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("Wzorzec 2: Slice dostępnych produktów od najtańszego, remisy po id")
    void solution2() {
        Slice<Product> second = repository.availableCheapestFirst(1, 4);
        assertThat(names(second)).containsExactly("Mysz", "Kubek termiczny", "Segregator", "Klawiatura");
        assertThat(second.hasNext()).isTrue();

        Slice<Product> third = repository.availableCheapestFirst(2, 4);
        assertThat(names(third)).containsExactly("Monitor", "Laptop");
        assertThat(third.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 3: n najdroższych produktów kategorii przez Limit")
    void solution3() {
        assertThat(names(repository.mostExpensive(Category.OFFICE, 2))).containsExactly("Krzesło biurowe", "Segregator");
        assertThat(names(repository.mostExpensive(Category.HOME, 5))).containsExactly("Lampka", "Kubek termiczny", "Kubek");
    }

    @Test
    @DisplayName("Wzorzec 4: wszystkie nazwy alfabetycznie, pobierane oknami po 5 (keyset)")
    void solution4() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        assertThat(repository.allNamesInWindowsOfFive()).containsExactly(
                "Długopis", "Klawiatura", "Krzesło biurowe", "Kubek", "Kubek termiczny", "Lampka",
                "Laptop", "Monitor", "Mysz", "Podkładka pod mysz", "Segregator", "Zeszyt");
        assertThat(stats.getPrepareStatementCount()).as("12 produktów oknami po 5 = 3 zapytania").isEqualTo(3);
    }
}
