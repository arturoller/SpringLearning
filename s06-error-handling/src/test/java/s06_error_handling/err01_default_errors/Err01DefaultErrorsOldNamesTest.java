package s06_error_handling.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import s06_error_handling.TestHttp;
import s06_error_handling.TestHttp.Reply;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PUŁAPKA: w Boocie 4 stare nazwy {@code server.error.*} (z tutoriali do Boota 2 i 3) zostały zastąpione przez
 * {@code spring.web.error.*}. Stare nazwy NIE zgłaszają błędu przy starcie — są po prostu ignorowane.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.error.include-message=always",
        "server.error.include-stacktrace=always"})
class Err01DefaultErrorsOldNamesTest {

    @LocalServerPort
    int port;

    @Test
    @DisplayName("1. server.error.include-message=always nic nie zmienia — odpowiedź nadal bez message i trace")
    void oldPropertyNamesAreIgnored() {
        Reply reply = TestHttp.get(port, "/api/products/KSI-999");

        assertThat(reply.status()).isEqualTo(500);
        assertThat(reply.json()).containsOnlyKeys("timestamp", "status", "error", "path");
    }
}
