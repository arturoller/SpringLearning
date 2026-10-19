package s08_data_jpa.solutions.jpa05_paging_sorting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import s08_data_jpa.jpa05_paging_sorting.Category;
import s08_data_jpa.jpa05_paging_sorting.Jpa05Application;
import s08_data_jpa.jpa05_paging_sorting.Product;
import s08_data_jpa.jpa05_paging_sorting.ProductRepository;
import s08_data_jpa.jpa05_paging_sorting.SampleProducts;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Jpa05PagingSortingExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze.
 * Rozwiązania używają ProductRepository z lekcji, więc {@code @ContextConfiguration} wskazuje klasę startową LEKCJI
 * (w pakiecie rozwiązań nie ma własnej klasy startowej — nie jest potrzebna).
 */
@DataJpaTest
@ContextConfiguration(classes = Jpa05Application.class)
@ActiveProfiles("jpa05")
@Tag("wzorzec")
class Jpa05PagingSortingSolutionsTest {

    @Autowired
    ProductRepository repository;

    @BeforeEach
    void insertShopProducts() {
        repository.saveAllAndFlush(SampleProducts.all());
    }

    @Test
    @DisplayName("Wzorzec 1: druga strona po 4, sortowanie po nazwie")
    void solution1() {
        Page<Product> page = Jpa05PagingSortingSolutions.solution1(repository);
        assertThat(page.getContent()).extracting(Product::getName)
                .containsExactly("Kawa ziarnista 1kg", "Kurtka zimowa", "Lampka biurkowa", "Laptop Pro 14");
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getTotalElements()).isEqualTo(14);
        assertThat(page.getTotalPages()).isEqualTo(4);
    }

    @Test
    @DisplayName("Wzorzec 2: przejście po wszystkich wycinkach (Slice) daje komplet SKU produktów na stanie")
    void solution2() {
        assertThat(Jpa05PagingSortingSolutions.solution2(repository, 5)).containsExactly(
                "DOM-001", "DOM-002", "ELE-001", "ELE-003", "ELE-004", "KSI-001",
                "KSI-002", "KSI-003", "ODZ-001", "ODZ-002", "SPO-001", "SPO-002");
        assertThat(Jpa05PagingSortingSolutions.solution2(repository, 4)).hasSize(12).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Wzorzec 3: druga strona po 7, cena malejąco, remisy rozstrzygnięte po sku")
    void solution3() {
        for (int attempt = 0; attempt < 10; attempt++) {
            assertThat(Jpa05PagingSortingSolutions.solution3(repository).getContent()).extracting(Product::getSku)
                    .containsExactly("KSI-002", "KSI-003", "KSI-001", "SPO-001", "ODZ-002", "SPO-003", "SPO-002");
        }
    }

    @Test
    @DisplayName("Wzorzec 4: strona nazw (Page<String>) z zachowanymi metadanymi")
    void solution4() {
        Page<String> names = Jpa05PagingSortingSolutions.solution4(repository, Category.SPOZYWCZE, 1, 2);
        assertThat(names.getContent()).containsExactly("Kawa ziarnista 1kg");
        assertThat(names.getNumber()).isEqualTo(1);
        assertThat(names.getTotalElements()).isEqualTo(3);
        assertThat(names.getTotalPages()).isEqualTo(2);
    }
}
