package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 3 lekcji Test01TestSlices: {@code @DataJpaTest} — wycinek JPA. Są encje, repozytoria, DataSource
 * (podmieniony na świeżą bazę wbudowaną H2) i transakcja na każdy test; NIE MA kontrolerów, serwisów ani MockMvc.
 * Dokładniej o tym wycinku: lekcja s11_testing/Test04DataJpaTest.
 */
@DataJpaTest
class Test01TestSlicesDataJpaTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    ProductRepository repository;

    @Test
    @DisplayName("3. @DataJpaTest: repozytorium jest, kontrolera, serwisu i MockMvc NIE MA")
    void jpaSliceContents() {
        assertThat(context.getBeanNamesForType(ProductRepository.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(ProductController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(ProductService.class)).isEmpty();
        assertThat(context.containsBean("mockMvc")).isFalse();
    }

    @Test
    @DisplayName("3. @DataJpaTest: zapytanie z nazwy metody działa na prawdziwej bazie H2")
    void derivedQuery() {
        repository.save(new Product("Kawa ziarnista 1kg", 6499));
        repository.save(new Product("Czekolada gorzka", 749));
        repository.save(new Product("Ekspres do kawy", 189900));

        assertThat(repository.findByNameContainingIgnoreCaseOrderByNameAsc("KAW"))   // „Kawa…” i „…kawy”
                .extracting(Product::getName)
                .containsExactly("Ekspres do kawy", "Kawa ziarnista 1kg");
    }
}
