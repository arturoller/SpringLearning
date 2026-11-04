package s06_error_handling.err03_controller_advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prawdziwy serwer: advice przejmuje błędy PRZED kontrolerem {@code /error}, więc nawet nieznana ścieżka dostaje
 * format {@code ApiError} (a nie domyślny JSON Boota z Err01DefaultErrors).
 */
@SpringBootTest(classes = Err03Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Err03ServerTest {

    @LocalServerPort
    int port;

    private record Reply(HttpStatusCode status, String body) {
    }

    private Reply get(String uri) {
        return RestClient.create("http://localhost:" + port).get().uri(uri).header("Accept", "application/json")
                .exchange((req, response) -> new Reply(response.getStatusCode(), new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Serwer: błąd domenowy, wyjątek nieprzewidziany i nieznana ścieżka — wszystkie w formacie ApiError")
    void everythingInOneFormat() {
        Reply notFound = get("/err03/products/9");
        Reply crash = get("/err03/products/crash");
        Reply unknownPath = get("/err03/nie-ma-takiej-sciezki");

        assertThat(notFound.status().value()).isEqualTo(404);
        assertThat(notFound.body()).isEqualTo("{\"status\":404,\"code\":\"PRODUCT_NOT_FOUND\",\"message\":\"Nie ma produktu o id 9\","
                + "\"path\":\"/err03/products/9\"}");
        assertThat(crash.status().value()).isEqualTo(500);
        assertThat(crash.body()).startsWith("{\"status\":500,\"code\":\"UNEXPECTED\"").doesNotContain("timestamp", "hasło");
        assertThat(unknownPath.status().value()).isEqualTo(404);
        assertThat(unknownPath.body()).startsWith("{\"status\":404,\"code\":\"HTTP_404\"").doesNotContain("timestamp");
    }
}
