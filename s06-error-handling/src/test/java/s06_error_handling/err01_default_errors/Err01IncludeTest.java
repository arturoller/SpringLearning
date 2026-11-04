package s06_error_handling.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 2 lekcji Err01DefaultErrors: właściwości {@code spring.web.error.include-*} (w Boot 4; w Boot 3 nazywały się
 * {@code server.error.include-*}). Ustawiamy je tylko w tym teście ({@code properties = …}), więc reszta lekcji zostaje
 * z ustawieniami domyślnymi.
 */
@SpringBootTest(classes = Err01Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.web.error.include-message=always",
        "spring.web.error.include-exception=true",
        "spring.web.error.include-stacktrace=on_param"})
class Err01IncludeTest {

    @LocalServerPort
    int port;

    private final ObjectMapper json = new ObjectMapper();

    private JsonNode getJson(String uri) {
        String body = RestClient.create("http://localhost:" + port).get().uri(uri).header("Accept", "application/json")
                .exchange((req, response) -> new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
        return json.readTree(body);
    }

    @Test
    @DisplayName("Sekcja 2: include-message=always dokłada pole message (i ujawnia komunikat wyjątku!)")
    void messageIsIncluded() {
        JsonNode body = getJson("/err01/boom");

        assertThat(body.get("message").asString()).isEqualTo("Błąd wewnętrzny: połączenie z bazą db-prod-01 odrzucone");
        assertThat(getJson("/err01/products/9").get("message").asString()).isEqualTo("Nie ma produktu o id 9");
    }

    @Test
    @DisplayName("Sekcja 2: include-exception=true dokłada nazwę klasy wyjątku")
    void exceptionClassIsIncluded() {
        assertThat(getJson("/err01/boom").get("exception").asString()).isEqualTo("java.lang.IllegalStateException");
    }

    @Test
    @DisplayName("Sekcja 2: include-stacktrace=on_param dokłada trace tylko, gdy zapytanie ma ?trace=true")
    void stackTraceOnlyOnParameter() {
        assertThat(getJson("/err01/boom").has("trace")).isFalse();
        JsonNode withTrace = getJson("/err01/boom?trace=true");
        assertThat(withTrace.get("trace").asString()).startsWith("java.lang.IllegalStateException")
                .contains("ShopErrorsController.boom");   // nazwy klas i numery linii Twojego kodu — wyciek informacji
    }
}
