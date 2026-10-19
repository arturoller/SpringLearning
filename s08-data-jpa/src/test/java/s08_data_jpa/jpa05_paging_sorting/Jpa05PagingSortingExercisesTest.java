package s08_data_jpa.jpa05_paging_sorting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Jpa05PagingSorting. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy albo
 * {@code mvnw -pl s08-data-jpa test -Dgroups=cwiczenie -DexcludedGroups=none}). Przed każdym testem w bazie
 * jest 14 produktów sklepu.
 */
@DataJpaTest
@ActiveProfiles("jpa05")
@Tag("cwiczenie")
class Jpa05PagingSortingExercisesTest {

    @Autowired
    ProductRepository repository;

    @BeforeEach
    void insertShopProducts() {
        repository.saveAllAndFlush(SampleProducts.all());
    }

    @Test
    @DisplayName("Ćwiczenie 1: druga strona po 4, sortowanie po nazwie")
    void exercise1() {
        Page<Product> page = Jpa05PagingSorting.exercise1(repository);
        assertThat(page.getContent()).extracting(Product::getName)
                .containsExactly("Kawa ziarnista 1kg", "Kurtka zimowa", "Lampka biurkowa", "Laptop Pro 14");
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getTotalElements()).isEqualTo(14);
        assertThat(page.getTotalPages()).isEqualTo(4);
    }

    @Test
    @DisplayName("Ćwiczenie 2: przejście po wszystkich wycinkach (Slice) daje komplet SKU produktów na stanie")
    void exercise2() {
        assertThat(Jpa05PagingSorting.exercise2(repository, 5)).containsExactly(
                "DOM-001", "DOM-002", "ELE-001", "ELE-003", "ELE-004", "KSI-001",
                "KSI-002", "KSI-003", "ODZ-001", "ODZ-002", "SPO-001", "SPO-002");
        assertThat(Jpa05PagingSorting.exercise2(repository, 4)).hasSize(12).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Ćwiczenie 3: druga strona po 7, cena malejąco, remisy rozstrzygnięte po sku")
    void exercise3() {
        // powtórzenie: przy niejednoznacznym sortowaniu H2 potrafi zwrócić inną kolejność remisu
        for (int attempt = 0; attempt < 10; attempt++) {
            assertThat(Jpa05PagingSorting.exercise3(repository).getContent()).extracting(Product::getSku)
                    .containsExactly("KSI-002", "KSI-003", "KSI-001", "SPO-001", "ODZ-002", "SPO-003", "SPO-002");
        }
    }

    @Test
    @DisplayName("Ćwiczenie 4: strona nazw (Page<String>) z zachowanymi metadanymi")
    void exercise4() {
        Page<String> names = Jpa05PagingSorting.exercise4(repository, Category.SPOZYWCZE, 1, 2);
        assertThat(names.getContent()).containsExactly("Kawa ziarnista 1kg");
        assertThat(names.getNumber()).isEqualTo(1);
        assertThat(names.getTotalElements()).isEqualTo(3);
        assertThat(names.getTotalPages()).isEqualTo(2);
    }
}
