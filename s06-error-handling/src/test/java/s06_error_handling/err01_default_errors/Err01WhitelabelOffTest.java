package s06_error_handling.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Sekcja 5 lekcji Err01DefaultErrors: wyłączona strona Whitelabel ({@code spring.web.error.whitelabel.enabled=false}). */
@SpringBootTest(classes = Err01Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.web.error.whitelabel.enabled=false")
class Err01WhitelabelOffTest {

    @LocalServerPort
    int port;

    @Test
    @DisplayName("Sekcja 5: bez Whitelabel przeglądarka dostaje stronę błędu samego Tomcata, a klient JSON dalej ten sam JSON")
    void tomcatPageInsteadOfWhitelabel() {
        RestClient client = RestClient.create("http://localhost:" + port);

        String html = client.get().uri("/err01/boom").header("Accept", "text/html")
                .exchange((req, response) -> new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
        String json = client.get().uri("/err01/boom").header("Accept", "application/json")
                .exchange((req, response) -> new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));

        assertThat(html).doesNotContain("Whitelabel").contains("HTTP Status 500");
        assertThat(json).contains("\"status\":500", "\"error\":\"Internal Server Error\"");
    }
}
