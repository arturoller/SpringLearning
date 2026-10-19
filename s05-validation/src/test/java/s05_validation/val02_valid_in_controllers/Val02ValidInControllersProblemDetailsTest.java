package s05_validation.val02_valid_in_controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 7b lekcji Val02ValidInControllers: to samo zapytanie, ale z właściwością
 * {@code spring.mvc.problemdetails.enabled=true}. Inna właściwość = inny kontekst Springa, więc osobna klasa testowa.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.mvc.problemdetails.enabled=true")
class Val02ValidInControllersProblemDetailsTest {

    @LocalServerPort
    int port;

    RestClient client;

    @BeforeEach
    void createClient() {
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    @DisplayName("7b. Z problemdetails.enabled=true odpowiedź ma format problem+json, ale nadal bez listy pól")
    @SuppressWarnings("unchecked")
    void section7b_problemDetailsStillWithoutFields() {
        record Answer(MediaType contentType, Map<String, Object> body) {
        }
        Answer answer = client.post().uri("/api/products").contentType(MediaType.APPLICATION_JSON)
                .body(Val02ValidInControllersTest.BAD_BODY)
                .exchange((req, res) -> new Answer(res.getHeaders().getContentType(), res.bodyTo(Map.class)));

        assertThat(answer.contentType().toString()).isEqualTo("application/problem+json");
        assertThat(new TreeSet<>(answer.body().keySet())).containsExactly("detail", "instance", "status", "title");
        assertThat(answer.body()).containsEntry("status", 400).containsEntry("title", "Bad Request")
                .containsEntry("detail", "Invalid request content.");
    }
}
