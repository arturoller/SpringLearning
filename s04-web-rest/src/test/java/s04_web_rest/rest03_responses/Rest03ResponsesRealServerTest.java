package s04_web_rest.rest03_responses;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lekcja Rest03Responses na PRAWDZIWYM serwerze (losowy port). Tu widać to, czego MockMvc nie pokaże:
 * port w nagłówku Location, JSON strony błędu Boota (bez tekstu powodu) i puste ciało przy 406.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Rest03ResponsesRealServerTest {

    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }

    @Test
    @DisplayName("2. Location na prawdziwym serwerze zawiera port, pod którym działa aplikacja")
    void locationContainsPort() throws Exception {
        HttpResponse<String> response = client.send(request("/api/products")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("""
                                {"sku":"KSI-002","name":"Java. Podstawy","price":129.00,"stock":9}
                                """))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Location"))
                .hasValue("http://localhost:" + port + "/api/products/KSI-002");
    }

    @Test
    @DisplayName("4. ResponseStatusException na serwerze → JSON błędu Boota bez tekstu powodu (message ukryte)")
    void errorJsonWithoutReason() throws Exception {
        HttpResponse<String> response = client.send(request("/api/products/XXX-999/details").build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body())
                .contains("\"status\":404", "\"error\":\"Not Found\"", "\"path\":\"/api/products/XXX-999/details\"")
                .doesNotContain("Nie ma produktu");
    }

    @Test
    @DisplayName("5. Accept: application/xml → 406 i PUSTE ciało; nagłówek Accept podpowiada dostępne typy (pytanie 5)")
    void notAcceptableHasEmptyBody() throws Exception {
        HttpResponse<String> response = client.send(request("/api/products/ELE-001")
                        .header("Accept", "application/xml").build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(406);
        assertThat(response.body()).isEmpty();
        assertThat(response.headers().firstValue("Accept")).get().asString()   // kolejność typów bywa różna
                .contains("application/json", "text/plain");
    }

    @Test
    @DisplayName("6. PUŁAPKA na serwerze: return null → 200 i Content-Length: 0 (pytanie 4)")
    void nullOnRealServer() throws Exception {
        HttpResponse<String> response = client.send(request("/api/pitfalls/null-product/XXX-999").build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEmpty();
        assertThat(response.headers().firstValue("Content-Length")).hasValue("0");
    }
}
