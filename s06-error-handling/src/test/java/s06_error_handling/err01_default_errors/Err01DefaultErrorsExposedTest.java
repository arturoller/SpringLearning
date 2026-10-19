package s06_error_handling.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import s06_error_handling.TestHttp;
import s06_error_handling.TestHttp.Reply;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ustawienia {@code spring.web.error.*} ustawione na „ujawniaj wszystko” — tak NIE konfigurujemy produkcji,
 * ale dzięki temu widać, co Boot chowa domyślnie.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.web.error.include-message=always",
        "spring.web.error.include-binding-errors=always",
        "spring.web.error.include-stacktrace=always",
        "spring.web.error.include-exception=true"})
class Err01DefaultErrorsExposedTest {

    @LocalServerPort
    int port;

    @Test
    @DisplayName("1. Po włączeniu ustawień odpowiedź 500 zawiera exception, trace i message z wnętrzem aplikacji")
    void exposesInternals() {
        Reply reply = TestHttp.get(port, "/api/products/KSI-999");

        assertThat(reply.status()).isEqualTo(500);
        assertThat(reply.json()).containsKeys("exception", "trace", "message");
        assertThat(reply.json().get("exception")).isEqualTo("java.lang.IllegalStateException");
        assertThat(reply.json().get("message").toString()).contains("jdbc:h2:mem:sklep");
        assertThat(reply.json().get("trace").toString()).contains("ProductController.one");
    }

    @Test
    @DisplayName("2. include-binding-errors=always dodaje tablicę errors z polami, które nie przeszły walidacji")
    void exposesBindingErrors() {
        Reply reply = TestHttp.postJson(port, "/api/products", "{\"name\":\"\",\"price\":-1}");

        assertThat(reply.status()).isEqualTo(400);
        assertThat(reply.json()).containsKey("errors");
        assertThat(reply.body()).contains("\"field\":\"name\"").contains("\"field\":\"price\"")
                .contains("\"code\":\"NotBlank\"").contains("\"code\":\"Positive\"");
    }
}
