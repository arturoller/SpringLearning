package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prawdziwy serwer: wszystkie błędy przechodzą przez {@code Err04Advice}, więc nawet nieznana ścieżka dostaje
 * {@code application/problem+json} zamiast domyślnego JSON-a z {@code /error} (Err01DefaultErrors, pole {@code timestamp}).
 */
@SpringBootTest(classes = Err04Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("err04")
class Err04ServerTest {

    @LocalServerPort
    int port;

    private final ObjectMapper json = new ObjectMapper();

    private record Reply(HttpStatusCode status, HttpHeaders headers, String body) {
    }

    private Reply send(String method, String uri, String accept) {
        RestClient.RequestHeadersSpec<?> request = RestClient.create("http://localhost:" + port)
                .method(org.springframework.http.HttpMethod.valueOf(method)).uri(uri);
        if (accept != null) {
            request = request.header("Accept", accept);
        }
        return request.exchange((req, response) -> new Reply(response.getStatusCode(), response.getHeaders(),
                new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private List<String> fieldNames(String body) {
        List<String> names = new ArrayList<>();
        json.readTree(body).propertyNames().forEach(names::add);
        Collections.sort(names);
        return names;
    }

    @Test
    @DisplayName("Serwer: błąd domenowy → application/problem+json z dokładnie tymi polami, także gdy klient prosi o zwykły JSON")
    void problemJsonOnTheWire() {
        Reply plain = send("GET", "/err04/products/9", null);
        Reply wantsJson = send("GET", "/err04/products/9", "application/json");

        assertThat(plain.status().value()).isEqualTo(404);
        assertThat(plain.headers().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(fieldNames(plain.body())).containsExactly("code", "detail", "instance", "status", "title", "type");
        JsonNode node = json.readTree(plain.body());
        assertThat(node.get("instance").asString()).isEqualTo("/err04/products/9");
        assertThat(node.get("code").asString()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(wantsJson.headers().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);   // Accept nie zmienia typu
    }

    @Test
    @DisplayName("Serwer: 405 z nagłówkiem Allow, nieznana ścieżka i nieoczekiwany wyjątek — wszystkie jako ProblemDetail, bez timestamp i wycieku")
    void springAndUnexpectedErrors() {
        Reply notAllowed = send("DELETE", "/err04/products/1", null);
        Reply unknownPath = send("GET", "/err04/nie-ma-takiej-sciezki", null);
        Reply crash = send("GET", "/err04/products/crash", null);

        assertThat(notAllowed.status().value()).isEqualTo(405);
        assertThat(notAllowed.headers().getFirst("Allow")).isEqualTo("GET");
        assertThat(notAllowed.headers().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(unknownPath.status().value()).isEqualTo(404);
        assertThat(unknownPath.headers().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(unknownPath.body()).doesNotContain("timestamp");
        assertThat(crash.status().value()).isEqualTo(500);
        assertThat(crash.body()).contains("UNEXPECTED").doesNotContain("hasło", "timestamp", "trace");
    }
}
