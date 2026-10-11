package s06_error_handling.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 3 lekcji Err01DefaultErrors: skutki WŁĄCZENIA szczegółów w błędzie. Klucze {@code spring.web.error.*}
 * ustawiamy tylko w tym teście ({@code properties = ...}), nie we wspólnym application.properties.
 * W starszych wersjach te klucze nazywały się {@code server.error.*} — w Boot 4 są przestarzałe.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.web.error.include-message=always",
        "spring.web.error.include-exception=true",
        "spring.web.error.include-stacktrace=on_param",
        "spring.web.error.include-path=never"
})
class Err01DefaultErrorsLeakyTest {

    @LocalServerPort
    int port;

    private Map<String, Object> getBody(String uri) {
        ResponseEntity<Map<String, Object>> response = RestClient.create("http://localhost:" + port).get().uri(uri)
                .retrieve()
                .onStatus(status -> true, (request, resp) -> {
                })
                .toEntity(new ParameterizedTypeReference<>() {
                });
        return response.getBody();
    }

    @Test
    @DisplayName("3. include-message=always i include-exception=true wyciekają komunikat i klasę wyjątku; include-path=never ukrywa path")
    void messageAndExceptionLeak() {
        Map<String, Object> body = getBody("/api/boom");

        assertThat(body).containsEntry("exception", "java.lang.IllegalStateException");
        assertThat((String) body.get("message")).contains("jdbc:postgresql://db.intranet:5432/shop", "shop_user");
        assertThat(body).doesNotContainKeys("path", "trace");   // stacktrace on_param: bez ?trace=true go nie ma
    }

    @Test
    @DisplayName("3. include-stacktrace=on_param pokazuje cały stacktrace po dopisaniu ?trace=true do adresu")
    void stacktraceOnParam() {
        Map<String, Object> body = getBody("/api/boom?trace=true");

        assertThat((String) body.get("trace")).contains("Err01Controller.boom");   // nazwy klas i metod serwera
    }
}
