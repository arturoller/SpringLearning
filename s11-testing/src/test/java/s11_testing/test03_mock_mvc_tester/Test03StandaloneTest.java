package s11_testing.test03_mock_mvc_tester;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 1 lekcji Test03MockMvcTester: MockMvcTester BEZ Springa i bez kontekstu — {@code MockMvcTester.of(kontroler)}
 * buduje "samodzielny" (standalone) MockMvc tylko z podanych kontrolerów. Najtańszy test kontrolera: mapowania,
 * statusy i JSON w milisekundach. Cena: nie działają adnotacje wymagające kontekstu Springa (np. własne advice, filtry,
 * konfiguracja z aplikacji).
 */
class Test03StandaloneTest {

    private final MockMvcTester mvc = MockMvcTester.of(new ProductController(new ProductService()));

    @Test
    @DisplayName("Sekcja 1: standalone — GET zwraca JSON bez uruchamiania kontekstu Springa")
    void standaloneGet() {
        assertThat(mvc.get().uri("/test03/products/1"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.name").asString().isEqualTo("Kawa");
    }

    @Test
    @DisplayName("Sekcja 1: standalone nie zna naszego advice — brak produktu to nieobsłużony wyjątek, a nie 404")
    void standaloneHasNoAdvice() {
        assertThat(mvc.get().uri("/test03/products/99"))
                .hasFailed()
                .failure().hasRootCauseInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("Sekcja 1: standalone z ręcznie podanym advice — wtedy brak produktu to 404 z ProblemDetail")
    void standaloneWithAdvice() {
        MockMvcTester withAdvice = MockMvcTester.of(List.of(new ProductController(new ProductService())),
                builder -> builder.setControllerAdvice(new ProductErrorAdvice()).build());

        assertThat(withAdvice.get().uri("/test03/products/99"))
                .hasStatus(404)
                .bodyJson().extractingPath("$.title").asString().isEqualTo("Nie ma takiego produktu");
    }
}
