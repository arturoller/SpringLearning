package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 2 lekcji Test01TestSlices: wycinek {@code @DataJpaTest} — tylko warstwa danych: encje, repozytoria, baza,
 * Flyway. Nie ma kontrolerów ani serwisów. Pełna lekcja o tym wycinku: s11_testing/Test04DataJpaTest.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test01")
class Test01DataJpaSliceTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    ProductRepository repository;

    @Test
    @DisplayName("Sekcja 2: w wycinku DataJpa jest repozytorium z danymi z Flyway, a kontrolera, serwisu i MVC nie ma")
    void sliceContents() {
        assertThat(repository.count()).as("dwa produkty z migracji V2").isEqualTo(2);
        assertThat(context.getBeanNamesForType(ProductController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(ProductService.class)).isEmpty();
        assertThat(context.getBeanNamesForType(DispatcherServlet.class)).isEmpty();
    }
}
