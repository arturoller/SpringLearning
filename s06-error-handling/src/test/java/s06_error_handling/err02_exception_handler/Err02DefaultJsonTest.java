package s06_error_handling.err02_exception_handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import s06_error_handling.TestHttp;
import s06_error_handling.TestHttp.Reply;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prawdziwy serwer (RANDOM_PORT): wyjątki z {@code @ResponseStatus} i {@code ResponseStatusException} NIE budują ciała
 * same — ustawiają tylko status i przekazują sprawę do /error (domyślny JSON z lekcji Err01DefaultErrors).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Err02DefaultJsonTest {

    @LocalServerPort
    int port;

    @Test
    @DisplayName("1. @ResponseStatus i ResponseStatusException kończą się domyślnym JSON-em Boota (bez message)")
    void statusOnlyExceptionsGiveDefaultJson() {
        for (String path : new String[]{"/api/products/XXX", "/api/products/ELE-001/reserve", "/api/products/ELE-001/offer"}) {
            Reply reply = TestHttp.get(port, path);
            assertThat(reply.json()).as(path).containsOnlyKeys("timestamp", "status", "error", "path");
        }
        assertThat(TestHttp.get(port, "/api/products/XXX").status()).isEqualTo(404);
        assertThat(TestHttp.get(port, "/api/products/ELE-001/reserve").status()).isEqualTo(409);
        assertThat(TestHttp.get(port, "/api/products/ELE-001/offer").status()).isEqualTo(410);
    }

    @Test
    @DisplayName("2. Handler z kontrolera omija /error — klient dostaje nasze ciało {code, message}")
    void handlerBodyReachesClient() {
        Reply reply = TestHttp.send("POST", port, "/api/payments/card", "application/json", null, null);

        assertThat(reply.status()).isEqualTo(402);
        assertThat(reply.json()).containsOnlyKeys("code", "message").containsEntry("code", "KARTA_ODRZUCONA");
    }

    @Test
    @DisplayName("3. Wyjątek z innego kontrolera (bez handlera) → 500 i domyślny JSON")
    void unhandledInOtherController() {
        Reply reply = TestHttp.send("POST", port, "/api/orders/ZAM-001/pay", "application/json", null, null);

        assertThat(reply.status()).isEqualTo(500);
        assertThat(reply.json()).containsOnlyKeys("timestamp", "status", "error", "path");
    }
}
