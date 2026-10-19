package s06_error_handling.err02_exception_handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import s06_error_handling.TestHttp;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Po włączeniu {@code spring.web.error.include-message=always} pole message pokazuje: komunikat wyjątku (dla
 * {@code @ResponseStatus} bez reason i dla ResponseStatusException) albo — gdy ustawiono {@code reason} — ten stały powód.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.web.error.include-message=always")
class Err02MessageVisibilityTest {

    @LocalServerPort
    int port;

    @Test
    @DisplayName("1. message: tekst wyjątku (bez reason), powód z ResponseStatusException, a przy reason — stały powód")
    void messageSources() {
        assertThat(TestHttp.get(port, "/api/products/XXX").json()).containsEntry("message", "Nie ma produktu o SKU XXX");
        assertThat(TestHttp.get(port, "/api/products/ELE-001/reserve").json())
                .containsEntry("message", "Towar ELE-001 jest chwilowo niedostępny");
        assertThat(TestHttp.get(port, "/api/products/ELE-001/offer").json())
                .containsEntry("message", "Produkt wycofany ze sprzedaży");   // reason wygrywa z tekstem wyjątku
    }
}
