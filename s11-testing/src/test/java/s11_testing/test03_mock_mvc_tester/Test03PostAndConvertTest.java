package s11_testing.test03_mock_mvc_tester;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcje 3 i 4 lekcji Test03MockMvcTester: żądanie POST z JSON-em, odpowiedź 201 z nagłówkiem Location, błąd
 * walidacji 400 z treścią ProblemDetail oraz zamiana odpowiedzi na obiekt ({@code convertTo}).
 */
@WebMvcTest(ProductController.class)
@Import(ProductService.class)
class Test03PostAndConvertTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    ProductService service;

    @BeforeEach
    void reset() {
        service.reset();
    }

    @Test
    @DisplayName("Sekcja 3: POST z JSON-em → 201, nagłówek Location i utworzony produkt w ciele")
    void postCreates() {
        assertThat(mvc.post().uri("/test03/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ciastka\",\"price\":12.50}"))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "http://localhost/test03/products/3")
                .bodyJson().isLenientlyEqualTo("{\"id\":3,\"name\":\"Ciastka\",\"price\":12.50}");
    }

    @Test
    @DisplayName("Sekcja 3: błąd walidacji → 400 i ProblemDetail z błędami po polach (title, errors.name, errors.price)")
    void validationError() {
        assertThat(mvc.post().uri("/test03/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \",\"price\":-1}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType("application/problem+json")
                .bodyJson()
                .hasPathSatisfying("$.title", title -> title.assertThat().asString().isEqualTo("Niepoprawne dane"))
                .extractingPath("$.errors.name").asString().isEqualTo("nazwa nie może być pusta");

        assertThat(mvc.post().uri("/test03/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \",\"price\":-1}"))
                .bodyJson().extractingPath("$.errors.price").asString().isEqualTo("cena nie może być ujemna");
    }

    @Test
    @DisplayName("Sekcja 3: zły JSON (niedomknięty nawias) → 400 jeszcze przed kontrolerem — produkt nie powstaje")
    void malformedJson() {
        assertThat(mvc.post().uri("/test03/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ciastka\""))
                .hasStatus(HttpStatus.BAD_REQUEST);

        assertThat(service.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Sekcja 3: brak nagłówka Content-Type przy POST z ciałem → 415 Unsupported Media Type")
    void missingContentType() {
        assertThat(mvc.post().uri("/test03/products").content("{\"name\":\"Ciastka\",\"price\":12.50}"))
                .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    @DisplayName("Sekcja 4: convertTo(ProductResponse.class) — odpowiedź JSON jako obiekt, dalej asercje na polach")
    void convertToObject() {
        assertThat(mvc.get().uri("/test03/products/1")).bodyJson()
                .convertTo(ProductResponse.class)
                .isEqualTo(new ProductResponse(1, "Kawa", new BigDecimal("24.90")));
    }

    @Test
    @DisplayName("Sekcja 4: lista — convertTo(InstanceOfAssertFactories.list(ProductResponse.class)) daje asercje AssertJ na liście")
    void convertToList() {
        assertThat(mvc.get().uri("/test03/products")).bodyJson()
                .convertTo(InstanceOfAssertFactories.list(ProductResponse.class))
                .hasSize(2)
                .extracting(ProductResponse::name).containsExactly("Kawa", "Herbata");
    }

    @Test
    @DisplayName("Sekcja 4: wynik żądania można wziąć do zmiennej (exchange()) i użyć kilka razy — jedno wywołanie kontrolera")
    void exchangeOnce() throws Exception {
        var result = mvc.get().uri("/test03/products/1").exchange();   // żądanie wykonane TERAZ, jeden raz

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.name").asString().isEqualTo("Kawa");
        assertThat(result.getResponse().getContentAsString()).contains("Kawa");
    }
}
