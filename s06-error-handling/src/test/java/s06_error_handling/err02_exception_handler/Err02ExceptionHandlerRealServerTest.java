package s06_error_handling.err02_exception_handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Sekcje 1–2 na prawdziwym serwerze: jak wygląda ciało błędu z {@code @ResponseStatus} i {@code ResponseStatusException}. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Err02ExceptionHandlerRealServerTest {

    @LocalServerPort
    int port;

    private ResponseEntity<Map<String, Object>> get(String uri) {
        return RestClient.create("http://localhost:" + port).get().uri(uri)
                .retrieve()
                .onStatus(status -> true, (request, response) -> {
                })
                .toEntity(new ParameterizedTypeReference<>() {
                });
    }

    @Test
    @DisplayName("1–2. Na serwerze oba mechanizmy kończą na /error: właściwy status, ale reason NIE jest w ciele (include-message=never)")
    void bodyComesFromErrorController() {
        ResponseEntity<Map<String, Object>> notFound = get("/api/catalog/999");
        ResponseEntity<Map<String, Object>> conflict = get("/api/catalog/5/stock");

        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFound.getBody()).containsOnlyKeys("timestamp", "status", "error", "path");
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody()).doesNotContainKey("message");
    }

    @Test
    @DisplayName("3. Wyjątek obsłużony przez @ExceptionHandler NIE przechodzi przez /error — ciało jest nasze")
    void handledBodyIsOurs() {
        ResponseEntity<Map<String, Object>> response = get("/api/catalog/discount?percent=150");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsOnlyKeys("error");
    }
}
