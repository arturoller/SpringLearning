package s11_testing.solutions.test03_mock_mvc_tester;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import s11_testing.test03_mock_mvc_tester.ProductController;
import s11_testing.test03_mock_mvc_tester.ProductErrorAdvice;
import s11_testing.test03_mock_mvc_tester.ProductService;

import static org.assertj.core.api.Assertions.assertThat;

/** Rozwiązania wzorcowe ćwiczeń Test03MockMvcTester. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
@WebMvcTest
@Import({ProductController.class, ProductErrorAdvice.class, ProductService.class})
class Test03MockMvcTesterSolutionsTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    ProductService service;

    @BeforeEach
    void reset() {
        service.reset();
    }

    @Test
    @DisplayName("Wzorzec 1: GET → 200 i nazwa Herbata")
    void exercise1() {
        assertThat(mvc.get().uri("/test03/products/2"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.name").asString().isEqualTo("Herbata");
    }

    @Test
    @DisplayName("Wzorzec 2: brak produktu → 404, application/problem+json i tytuł")
    void exercise2() {
        assertThat(mvc.get().uri("/test03/products/99"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType("application/problem+json")
                .bodyJson().extractingPath("$.title").asString().isEqualTo("Nie ma takiego produktu");
    }

    @Test
    @DisplayName("Wzorzec 3: POST → 201, Location i JSON z nazwą")
    void exercise3() {
        assertThat(mvc.post().uri("/test03/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ciastka\",\"price\":12.50}"))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "http://localhost/test03/products/3")
                .bodyJson().isLenientlyEqualTo("{\"name\":\"Ciastka\"}");
    }

    @Test
    @DisplayName("Wzorzec 4: /boom → porażka z przyczyną IllegalStateException")
    void exercise4() {
        assertThat(mvc.get().uri("/test03/boom"))
                .hasFailed()
                .failure().hasRootCauseInstanceOf(IllegalStateException.class).hasRootCauseMessage("Awaria serwisu");
    }

    @Test
    @DisplayName("Wzorzec 5: zła nazwa i cena → 400 z błędami obu pól")
    void exercise5() {
        assertThat(mvc.post().uri("/test03/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \",\"price\":-1}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().hasPath("$.errors.name").hasPath("$.errors.price");
    }
}
