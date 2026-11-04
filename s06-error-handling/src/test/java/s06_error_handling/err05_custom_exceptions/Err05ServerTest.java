package s06_error_handling.err05_custom_exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Err05CustomExceptions: wszystko razem na prawdziwym serwerze, z PRAWDZIWYM serwisem (bez atrap). Używamy tylko
 * żądań, które nie zmieniają danych (zawsze kończą się tym samym błędem), więc kolejność testów nie ma znaczenia.
 */
@SpringBootTest(classes = Err05Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Err05ServerTest {

    @LocalServerPort
    int port;

    private final ObjectMapper json = new ObjectMapper();

    private record Reply(HttpStatusCode status, HttpHeaders headers, JsonNode body) {
    }

    private Reply send(HttpMethod method, String uri, String body) {
        RestClient.RequestBodySpec request = RestClient.create("http://localhost:" + port).method(method).uri(uri);
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return request.exchange((req, response) -> new Reply(response.getStatusCode(), response.getHeaders(),
                json.readTree(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8))));
    }

    @Test
    @DisplayName("Serwer: cztery błędy domenowe z prawdziwego serwisu — 404, 409, 409, 422 w jednym formacie ProblemDetail")
    void domainErrorsEndToEnd() {
        Reply noProduct = send(HttpMethod.GET, "/err05/products/9", null);
        Reply noStock = send(HttpMethod.POST, "/err05/orders", "{\"productId\":2,\"quantity\":2}");   // Herbata: 0 sztuk
        Reply shipped = send(HttpMethod.POST, "/err05/orders/2/ship", null);
        Reply tooSmall = send(HttpMethod.POST, "/err05/orders", "{\"productId\":1,\"quantity\":1}");

        assertThat(noProduct.status().value()).isEqualTo(404);
        assertThat(noProduct.headers().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(noProduct.body().get("code").asString()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(noStock.status().value()).isEqualTo(409);
        assertThat(noStock.body().get("code").asString()).isEqualTo("OUT_OF_STOCK");
        assertThat(shipped.status().value()).isEqualTo(409);
        assertThat(shipped.body().get("code").asString()).isEqualTo("ORDER_ALREADY_SHIPPED");
        assertThat(tooSmall.status().value()).isEqualTo(422);
        assertThat(tooSmall.body().get("code").asString()).isEqualTo("MINIMUM_ORDER_VALUE");
    }

    @Test
    @DisplayName("Serwer: 500 nie zdradza szczegółów, a 'czy istnieje?' to zwykłe 200")
    void unexpectedAndExists() {
        Reply crash = send(HttpMethod.GET, "/err05/crash", null);
        Reply exists = send(HttpMethod.GET, "/err05/products/9/exists", null);

        assertThat(crash.status().value()).isEqualTo(500);
        assertThat(crash.body().get("code").asString()).isEqualTo("UNEXPECTED");
        assertThat(crash.body().toString()).doesNotContain("hasło", "timestamp");
        assertThat(exists.status().value()).isEqualTo(200);
        assertThat(exists.body().get("exists").asBoolean()).isFalse();
    }
}
