package s06_error_handling.err01_default_errors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcje 1, 3 i 6 lekcji Err01DefaultErrors z PRAWDZIWYM serwerem ({@code RANDOM_PORT} = losowy wolny port). Tylko tu
 * działa mechanizm /error, bo wymaga kontenera serwletów (Tomcata). Domyślne ustawienia — nic nie zmieniamy.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Err01DefaultErrorsTest {

    @LocalServerPort
    int port;

    RestClient client;

    @BeforeEach
    void createClient() {
        client = RestClient.create("http://localhost:" + port);
    }

    /** Pobiera odpowiedź BEZ rzucania wyjątku dla 4xx/5xx — onStatus(zawsze) z pustą obsługą. */
    private ResponseEntity<Map<String, Object>> getJson(String uri) {
        return client.get().uri(uri)
                .retrieve()
                .onStatus(status -> true, (request, response) -> {   // true = „dla każdego statusu nie rób nic”
                })
                .toEntity(new ParameterizedTypeReference<>() {
                });
    }

    @Test
    @DisplayName("1. Niezłapany wyjątek → 500 i domyślny JSON: timestamp, status, error, path (bez komunikatu!)")
    void defaultBodyOf500() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/boom");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsOnlyKeys("timestamp", "status", "error", "path");   // wartości timestamp nie sprawdzamy
        assertThat(body).containsEntry("status", 500)
                .containsEntry("error", "Internal Server Error")
                .containsEntry("path", "/api/boom");
    }

    @Test
    @DisplayName("3. Domyślnie nie wyciekają: ani komunikat wyjątku, ani stacktrace, ani nazwa klasy wyjątku")
    void noLeaksByDefault() {
        Map<String, Object> body = getJson("/api/boom?trace=true").getBody();   // ?trace=true nic nie daje, gdy włączenie = never

        assertThat(body).doesNotContainKeys("message", "trace", "exception", "errors");
        assertThat(body.toString()).doesNotContain("jdbc", "shop_user");
    }

    @Test
    @DisplayName("1. Ten sam mechanizm obsługuje też 400 (zły typ parametru) i 404 (brak adresu)")
    void sameFormatFor400And404() {
        ResponseEntity<Map<String, Object>> badRequest = getJson("/api/products/abc");
        ResponseEntity<Map<String, Object>> notFound = getJson("/api/nie-ma");

        assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badRequest.getBody()).containsEntry("error", "Bad Request").containsEntry("path", "/api/products/abc");
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFound.getBody()).containsEntry("error", "Not Found").containsEntry("path", "/api/nie-ma");
    }

    @Test
    @DisplayName("6. Przeglądarka (Accept: text/html) dostaje stronę HTML „Whitelabel Error Page”, a nie JSON")
    void browserGetsHtml() {
        ResponseEntity<String> response = client.get().uri("/api/boom")
                .accept(MediaType.TEXT_HTML)
                .retrieve()
                .onStatus(status -> true, (request, resp) -> {
                })
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
        assertThat(response.getBody()).contains("Whitelabel Error Page");
    }
}
