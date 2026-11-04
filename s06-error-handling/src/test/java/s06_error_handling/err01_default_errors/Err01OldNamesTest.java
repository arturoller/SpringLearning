package s06_error_handling.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 2 lekcji Err01DefaultErrors: PUŁAPKA ze starych tutoriali. Nazwa {@code server.error.include-message} (Boot 3 i
 * starsze) w Boot 4 jest oznaczona jako usunięta (zastąpiona przez {@code spring.web.error.include-message}).
 */
@SpringBootTest(classes = Err01Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.error.include-message=always", "server.error.include-stacktrace=always"})
class Err01OldNamesTest {

    @LocalServerPort
    int port;

    @Test
    @DisplayName("Sekcja 2: stare nazwy server.error.include-* w Boot 4 nic nie robią — aplikacja startuje, ale ciało bez zmian")
    void oldPropertyNamesAreIgnored() {
        String body = RestClient.create("http://localhost:" + port).get().uri("/err01/boom").header("Accept", "application/json")
                .exchange((req, response) -> new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));

        assertThat(body).doesNotContain("\"message\"", "\"trace\"", "db-prod-01");
    }
}
