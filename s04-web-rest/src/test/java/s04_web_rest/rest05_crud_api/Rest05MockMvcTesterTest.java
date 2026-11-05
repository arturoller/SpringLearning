package s04_web_rest.rest05_crud_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Rest05CrudApi — pokaz MockMvcTester: ten sam MockMvc, ale z asercjami w stylu AssertJ
 * ({@code assertThat(...)}), z którymi zna Cię już reszta testów kursu. Pełna lekcja: s11_testing/Test03MockMvcTester.
 */
@WebMvcTest(ProductController.class)
@Import(ProductService.class)
class Rest05MockMvcTesterTest {

    @Autowired
    MockMvcTester mvc;       // @WebMvcTest tworzy go automatycznie obok MockMvc

    @Autowired
    ProductService service;

    @BeforeEach
    void reset() {
        service.reset();
    }

    @Test
    @DisplayName("MockMvcTester: GET → status i JSON przez assertThat; POST → 201 i nagłówek Location")
    void getAndPost() {
        assertThat(mvc.get().uri("/rest05/products/1"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.name").asString().isEqualTo("Kawa");

        assertThat(mvc.post().uri("/rest05/products").contentType("application/json")
                .content("{\"name\":\"Ciastka\",\"price\":12.50}"))
                .hasStatus(201)
                .hasHeader("Location", "http://localhost/rest05/products/3");

        assertThat(mvc.get().uri("/rest05/products/99")).hasStatus(404);
    }
}
