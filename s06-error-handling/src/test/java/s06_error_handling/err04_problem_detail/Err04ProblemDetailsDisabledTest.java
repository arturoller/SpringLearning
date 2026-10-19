package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import s06_error_handling.TestHttp;
import s06_error_handling.TestHttp.Reply;

import static org.assertj.core.api.Assertions.assertThat;

/** Ustawienia domyślne Boota (spring.mvc.problemdetails.enabled nie ustawione = false) i brak własnej porady. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "err04.own-advice=false")
class Err04ProblemDetailsDisabledTest {

    @LocalServerPort
    int port;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("1. Domyślnie żadna porada ResponseEntityExceptionHandler nie jest zarejestrowana")
    void noAdviceByDefault() {
        assertThat(context.getBeansOfType(ResponseEntityExceptionHandler.class)).isEmpty();
    }

    @Test
    @DisplayName("2. Wszystkie błędy mają stary kształt z lekcji Err01 (timestamp, status, error, path), nie problem+json")
    void errorsHaveBootDefaultShape() {
        Reply notAllowed = TestHttp.send("DELETE", port, "/api/products/ELE-001", "application/json", null, null);
        assertThat(notAllowed.status()).isEqualTo(405);
        assertThat(notAllowed.contentType()).startsWith("application/json");
        assertThat(notAllowed.json()).containsOnlyKeys("timestamp", "status", "error", "path");

        Reply conflict = TestHttp.postJson(port, "/api/products/ELE-001/reserve?quantity=10", "");
        assertThat(conflict.status()).isEqualTo(409);   // status z ErrorResponseException zachowany...
        assertThat(conflict.json()).containsOnlyKeys("timestamp", "status", "error", "path");   // ...ale ProblemDetail zgubiony
    }
}
