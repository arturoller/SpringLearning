package s06_error_handling.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import s06_error_handling.TestHttp;
import s06_error_handling.TestHttp.Reply;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Err01DefaultErrors — USTAWIENIA DOMYŚLNE. {@code RANDOM_PORT} (losowy port): prawdziwy Tomcat, więc
 * zapytanie przechodzi całą drogę błędu, łącznie z przekierowaniem na /error (MockMvc tego nie robi — patrz Err01MockMvcTest).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Err01DefaultErrorsTest {

    @LocalServerPort
    int port;

    @Test
    @DisplayName("1. Nieznany adres → 404 i JSON z polami timestamp, status, error, path (bez message)")
    void notFoundHasDefaultBody() {
        Reply reply = TestHttp.get(port, "/api/nie-ma-takiego");

        assertThat(reply.status()).isEqualTo(404);
        assertThat(reply.contentType()).startsWith("application/json");
        assertThat(reply.json()).containsOnlyKeys("timestamp", "status", "error", "path");
        assertThat(reply.json()).containsEntry("status", 404)
                .containsEntry("error", "Not Found")
                .containsEntry("path", "/api/nie-ma-takiego");
    }

    @Test
    @DisplayName("2. Nieobsłużony wyjątek w kontrolerze → 500 Internal Server Error, a szczegóły zostają na serwerze")
    void unhandledExceptionGives500WithoutDetails() {
        Reply reply = TestHttp.get(port, "/api/products/KSI-999");

        assertThat(reply.status()).isEqualTo(500);
        assertThat(reply.json()).containsOnlyKeys("timestamp", "status", "error", "path");
        assertThat(reply.json()).containsEntry("error", "Internal Server Error");
        // Wewnętrzne szczegóły (tekst wyjątku, stos wywołań) NIE trafiają do klienta
        assertThat(reply.body()).doesNotContain("jdbc:h2").doesNotContain("IllegalStateException").doesNotContain("trace");
    }

    @Test
    @DisplayName("3. Poprawny produkt → 200, a błędne dane i zły typ w adresie → 400 w tym samym kształcie")
    void clientMistakesGive400() {
        assertThat(TestHttp.get(port, "/api/products/ELE-001").status()).isEqualTo(200);

        Reply badType = TestHttp.get(port, "/api/orders/abc");
        assertThat(badType.status()).isEqualTo(400);
        assertThat(badType.json()).containsOnlyKeys("timestamp", "status", "error", "path");

        Reply invalid = TestHttp.postJson(port, "/api/products", "{\"name\":\"\",\"price\":-1}");
        assertThat(invalid.status()).isEqualTo(400);
        assertThat(invalid.json()).containsOnlyKeys("timestamp", "status", "error", "path");   // bez "errors" — domyślnie ukryte

        Reply brokenJson = TestHttp.postJson(port, "/api/products", "{niepoprawny");
        assertThat(brokenJson.status()).isEqualTo(400);
    }

    @Test
    @DisplayName("4. Zła metoda HTTP → 405 Method Not Allowed z nagłówkiem Allow")
    void wrongMethodGives405() {
        Reply reply = TestHttp.send("DELETE", port, "/api/products/ELE-001", "application/json", null, null);

        assertThat(reply.status()).isEqualTo(405);
        assertThat(reply.headers().firstValue("Allow")).contains("GET");
    }

    @Test
    @DisplayName("5. Przeglądarka (Accept: text/html) dostaje stronę Whitelabel zamiast JSON-a")
    void browserGetsWhitelabelPage() {
        Reply reply = TestHttp.get(port, "/api/nie-ma-takiego", "text/html");

        assertThat(reply.status()).isEqualTo(404);
        assertThat(reply.contentType()).startsWith("text/html");
        assertThat(reply.body()).contains("Whitelabel Error Page").contains("status=404");
    }

    @Test
    @DisplayName("6. Klient, który nie akceptuje ani JSON-a, ani HTML-a (np. application/xml) → pusta odpowiedź")
    void unsupportedAcceptGivesEmptyBody() {
        Reply reply = TestHttp.get(port, "/api/nie-ma-takiego", "application/xml");

        assertThat(reply.status()).isEqualTo(404);
        assertThat(reply.body()).isEmpty();
    }
}
