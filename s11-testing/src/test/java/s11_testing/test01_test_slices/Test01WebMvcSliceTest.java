package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 2 lekcji Test01TestSlices: wycinek {@code @WebMvcTest} — tylko warstwa webowa. Kontroler i advice są,
 * serwisu i repozytorium NIE MA (serwis podstawiamy atrapą — szczegóły w s11_testing/Test02MockitoBeans).
 */
@WebMvcTest(ProductController.class)
class Test01WebMvcSliceTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService service;   // atrapa zamiast prawdziwego serwisu (którego wycinek nie tworzy)

    @Autowired
    ObjectProvider<ProductRepository> repository;   // dostawca, który nie wymaga istnienia beana

    @Test
    @DisplayName("Sekcja 2: w wycinku WebMvc jest kontroler i advice, a repozytorium i JPA nie ma")
    void sliceContents() {
        assertThat(context.getBeanNamesForType(ProductController.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(ProductErrorAdvice.class)).as("advice należy do warstwy webowej").hasSize(1);
        assertThat(repository.getIfAvailable()).isNull();
        assertThat(context.getBeanNamesForType(jakarta.persistence.EntityManagerFactory.class)).isEmpty();
        assertThat(context.getBeanNamesForType(java.time.Clock.class)).as("@Configuration nie jest skanowana w wycinku").isEmpty();
    }

    @Test
    @DisplayName("Sekcja 2: advice działa w wycinku — wyjątek serwisu zamienia się na 404 z ProblemDetail")
    void adviceTurnsExceptionInto404() throws Exception {
        given(service.get(99L)).willThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/test01/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Nie ma takiego produktu"))
                .andExpect(jsonPath("$.detail").value("Nie ma produktu o numerze 99"));
    }
}
