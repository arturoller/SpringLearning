package s05_validation.val02_valid_in_controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Val02ValidInControllers na PRAWDZIWYM serwerze ({@code RANDOM_PORT}). Strona błędu {@code /error}
 * (to ona buduje treść odpowiedzi 400 i 500) jest mechanizmem kontenera serwletów, więc MockMvc jej nie renderuje.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Val02ValidInControllersServerTest {

    @LocalServerPort
    int port;

    RestClient client;

    @BeforeEach
    void createClient() {
        client = RestClient.create("http://localhost:" + port);
    }

    /** Wynik zapytania: status i ciało JSON jako mapa (bez rzucania wyjątku przy 4xx/5xx — exchange). */
    record Answer(HttpStatusCode status, Map<String, Object> body) {
    }

    @SuppressWarnings("unchecked")
    private Answer send(RestClient.RequestHeadersSpec<?> request) {
        return request.exchange((req, res) -> new Answer(res.getStatusCode(), res.bodyTo(Map.class)));
    }

    @Test
    @DisplayName("7a. Domyślna odpowiedź 400 na błąd walidacji: tylko timestamp, status, error, path — bez listy pól")
    void section7a_defaultBodyHasNoFieldDetails() {
        Answer answer = send(client.post().uri("/api/products").contentType(MediaType.APPLICATION_JSON)
                .body(Val02ValidInControllersTest.BAD_BODY));

        assertThat(answer.status().value()).isEqualTo(400);
        assertThat(new TreeSet<>(answer.body().keySet())).containsExactly("error", "path", "status", "timestamp");
        assertThat(answer.body()).containsEntry("status", 400).containsEntry("error", "Bad Request")
                .containsEntry("path", "/api/products");
    }

    @Test
    @DisplayName("5. @Validated na klasie kontrolera: na prawdziwym serwerze klient dostaje 500 zamiast 400")
    void section5_validatedControllerGives500() {
        Answer answer = send(client.get().uri("/api/validated?limit=0"));

        assertThat(answer.status().value()).isEqualTo(500);
        assertThat(answer.body()).containsEntry("error", "Internal Server Error");

        // ta sama reguła na kontrolerze BEZ @Validated → 400 (wbudowana walidacja metod)
        assertThat(send(client.get().uri("/api/products?limit=0")).status().value()).isEqualTo(400);
    }
}
