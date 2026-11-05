package s11_testing.test03_mock_mvc_tester;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 2 lekcji Test03MockMvcTester: katalog asercji na odpowiedzi — status, nagłówki, typ treści, JSON
 * (ścieżki, porównanie luźne i ścisłe, porównanie z plikiem). Wszystkie zaczynają się od
 * {@code assertThat(mvc.get().uri(...))}.
 */
@WebMvcTest(ProductController.class)
@Import(ProductService.class)
class Test03AssertionsTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    ProductService service;

    @BeforeEach
    void reset() {
        service.reset();
    }

    @Test
    @DisplayName("Sekcja 2: status — hasStatusOk(), hasStatus(HttpStatus), hasStatus(int), hasStatus4xxClientError()")
    void status() {
        assertThat(mvc.get().uri("/test03/products/1")).hasStatusOk();
        assertThat(mvc.get().uri("/test03/products/99")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/test03/products/99")).hasStatus(404).hasStatus4xxClientError();
    }

    @Test
    @DisplayName("Sekcja 2: typ treści i nagłówki — hasContentType, hasHeader, containsHeader")
    void headersAndContentType() {
        assertThat(mvc.get().uri("/test03/products/1"))
                .hasContentType(MediaType.APPLICATION_JSON)
                .containsHeader("Content-Type");

        assertThat(mvc.get().uri("/test03/products/99"))
                .hasContentType("application/problem+json")        // błąd to ProblemDetail, a nie zwykły JSON
                .hasHeader("Content-Type", "application/problem+json");
    }

    @Test
    @DisplayName("Sekcja 2: bodyJson().extractingPath — wartość spod ścieżki JSONPath, z typem (asString, asNumber…)")
    void extractingPath() {
        assertThat(mvc.get().uri("/test03/products/2")).bodyJson()
                .extractingPath("$.name").asString().isEqualTo("Herbata");
        assertThat(mvc.get().uri("/test03/products")).bodyJson()
                .extractingPath("$.length()").isEqualTo(2);
        assertThat(mvc.get().uri("/test03/products")).bodyJson()
                .extractingPath("$[*].name").asList().containsExactly("Kawa", "Herbata");
    }

    @Test
    @DisplayName("Sekcja 2: hasPath / doesNotHavePath / hasPathSatisfying — czy ścieżka w JSON istnieje i co zawiera")
    void paths() {
        assertThat(mvc.get().uri("/test03/products/1")).bodyJson()
                .hasPath("$.price")
                .doesNotHavePath("$.internalCode")
                .hasPathSatisfying("$.name", name -> name.assertThat().asString().startsWith("Ka"));
    }

    @Test
    @DisplayName("Sekcja 2: isLenientlyEqualTo — luźne porównanie JSON: dodatkowe pola w odpowiedzi nie przeszkadzają")
    void lenientComparison() {
        assertThat(mvc.get().uri("/test03/products/1")).bodyJson()
                .isLenientlyEqualTo("{\"name\":\"Kawa\"}");   // w odpowiedzi są jeszcze id i price
    }

    @Test
    @DisplayName("Sekcja 2: isStrictlyEqualTo — ścisłe porównanie: dodatkowe pole w odpowiedzi to błąd; liczy się też kolejność w tablicy")
    void strictComparison() {
        assertThat(mvc.get().uri("/test03/products/1")).bodyJson()
                .isStrictlyEqualTo("{\"id\":1,\"name\":\"Kawa\",\"price\":24.90}");

        assertThatThrownBy(() -> assertThat(mvc.get().uri("/test03/products/1")).bodyJson()
                .isStrictlyEqualTo("{\"name\":\"Kawa\"}"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected");   // komunikat o nieoczekiwanych polach
    }

    @Test
    @DisplayName("Sekcja 2: porównanie z plikiem JSON z src/test/resources (isLenientlyEqualTo(Resource))")
    void compareWithFile() {
        assertThat(mvc.get().uri("/test03/products")).bodyJson()
                .isLenientlyEqualTo(new ClassPathResource("test03/products.json"));
    }

    @Test
    @DisplayName("Sekcja 2: bodyText() — treść jako tekst (gdy to nie JSON); pusta treść przy 404 bez opisu błędu")
    void bodyText() {
        assertThat(mvc.get().uri("/test03/products/1")).bodyText().contains("Kawa");
        assertThat(mvc.get().uri("/test03/products/99")).bodyText().contains("Nie ma produktu o numerze 99");
    }
}
