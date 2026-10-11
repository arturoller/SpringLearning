package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 3 lekcji Test01TestSlices: {@code @DataJpaTest} — wycinek JPA. Są encje, repozytoria, DataSource
 * (podmieniony na bazę wbudowaną) i transakcja na każdy test; NIE MA kontrolerów, serwisów ani MockMvc.
 */
@DataJpaTest
class Test01DataJpaSliceTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    ProductRepository repository;

    @Test
    @DisplayName("3. @DataJpaTest: repozytorium jest, kontrolera i serwisu NIE MA")
    void jpaSliceContents() {
        assertThat(context.getBeanNamesForType(ProductRepository.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(ProductController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(ProductService.class)).isEmpty();
        assertThat(context.containsBean("mockMvc")).isFalse();
    }

    @Test
    @DisplayName("3. @DataJpaTest: zapytanie pochodne działa na prawdziwej bazie H2")
    void derivedQuery() {
        repository.save(new Product("Kawa ziarnista", 4999));
        repository.save(new Product("Herbata", 1599));
        repository.save(new Product("kawa mielona", 2999));

        assertThat(repository.findByNameContainingIgnoreCaseOrderByNameAsc("KAWA"))
                .extracting(Product::getName)
                .containsExactly("Kawa ziarnista", "kawa mielona");
    }
}
