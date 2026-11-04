package s06_error_handling.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcje 1, 3 i 5 lekcji Err01DefaultErrors na PRAWDZIWYM serwerze. {@code @SpringBootTest(RANDOM_PORT)} startuje Tomcata na
 * losowym porcie, a {@link RestClient} wysyła prawdziwe zapytania HTTP — tylko tak przejdą one przez kontroler błędów
 * {@code /error} (MockMvc go nie uruchamia, patrz Err01MockMvcTest).
 */
@SpringBootTest(classes = Err01Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Err01DefaultErrorsTest {

    @LocalServerPort
    int port;

    private final ObjectMapper json = new ObjectMapper();

    private record Reply(HttpStatusCode status, MediaType contentType, String body) {

        int code() {
            return status.value();
        }
    }

    private Reply get(String uri, String accept) {
        RestClient.RequestHeadersSpec<?> request = RestClient.create("http://localhost:" + port).get().uri(uri).header("Accept", accept);
        return request.exchange((req, response) -> new Reply(response.getStatusCode(), response.getHeaders().getContentType(),
                new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private Reply getJson(String uri) {
        return get(uri, "application/json");
    }

    private List<String> fieldNames(Reply reply) {
        List<String> names = new ArrayList<>(json.readTree(reply.body()).propertyNames());
        Collections.sort(names);
        return names;
    }

    @Test
    @DisplayName("Sekcja 1: wyjątek bez obsługi → 500, a ciało to JSON z /error z czterema polami (bez message i trace)")
    void unhandledExceptionIs500() {
        Reply reply = getJson("/err01/boom");

        assertThat(reply.code()).isEqualTo(500);
        assertThat(reply.contentType().toString()).startsWith("application/json");
        assertThat(fieldNames(reply)).containsExactly("error", "path", "status", "timestamp");
        JsonNode body = json.readTree(reply.body());
        assertThat(body.get("status").asInt()).isEqualTo(500);
        assertThat(body.get("error").asString()).isEqualTo("Internal Server Error");
        assertThat(body.get("path").asString()).isEqualTo("/err01/boom");
        assertThat(reply.body()).doesNotContain("db-prod-01");   // tajny szczegół techniczny nie wycieka do klienta
    }

    @Test
    @DisplayName("Sekcja 1: nieznana ścieżka → 404 w tym samym kształcie JSON")
    void unknownPathIs404() {
        Reply reply = getJson("/err01/nie-ma-takiej-sciezki");

        assertThat(reply.code()).isEqualTo(404);
        assertThat(fieldNames(reply)).containsExactly("error", "path", "status", "timestamp");
        assertThat(json.readTree(reply.body()).get("error").asString()).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("Sekcja 1: parametr ?trace=true domyślnie NIE dodaje stack trace")
    void traceParameterIsIgnoredByDefault() {
        Reply reply = getJson("/err01/boom?trace=true");

        assertThat(reply.code()).isEqualTo(500);
        assertThat(fieldNames(reply)).doesNotContain("trace");
        assertThat(reply.body()).doesNotContain("ShopErrorsController");
    }

    @Test
    @DisplayName("Sekcja 3: @ResponseStatus na wyjątku → 409 Conflict, a opis wyjątku NIE trafia do ciała")
    void responseStatusOnException() {
        Reply reply = getJson("/err01/stock/kawa");

        assertThat(reply.code()).isEqualTo(409);
        assertThat(json.readTree(reply.body()).get("error").asString()).isEqualTo("Conflict");
        assertThat(fieldNames(reply)).doesNotContain("message");
        assertThat(reply.body()).doesNotContain("Brak towaru");
    }

    @Test
    @DisplayName("Sekcja 3: ResponseStatusException → 404 Not Found, a opis (reason) domyślnie NIE trafia do ciała")
    void responseStatusException() {
        Reply reply = getJson("/err01/products/9");

        assertThat(reply.code()).isEqualTo(404);
        assertThat(json.readTree(reply.body()).get("error").asString()).isEqualTo("Not Found");
        assertThat(reply.body()).doesNotContain("Nie ma produktu");
        assertThat(getJson("/err01/products/1").code()).isEqualTo(200);
    }

    @Test
    @DisplayName("Sekcja 5: przeglądarka (Accept: text/html) dostaje stronę Whitelabel zamiast JSON-a")
    void browserGetsWhitelabelPage() {
        Reply reply = get("/err01/boom", "text/html");

        assertThat(reply.code()).isEqualTo(500);
        assertThat(reply.contentType().toString()).startsWith("text/html");
        assertThat(reply.body()).contains("Whitelabel Error Page", "status=500");
        assertThat(reply.body()).doesNotContain("db-prod-01");
    }
}
