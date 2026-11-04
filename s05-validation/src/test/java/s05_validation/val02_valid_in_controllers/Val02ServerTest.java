package s05_validation.val02_valid_in_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Co dostaje PRAWDZIWY klient. {@code @SpringBootTest(RANDOM_PORT)} startuje Tomcata na losowym porcie, a {@link RestClient}
 * wysyła prawdziwe zapytania HTTP (jak w s04_web_rest/Rest05CrudApi). W {@code @WebMvcTest} ciało błędu 400 jest puste,
 * bo nie ma tam kontrolera błędów {@code /error}; na prawdziwym serwerze Boot dokłada ogólny JSON.
 */
@SpringBootTest(classes = Val02Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Val02ServerTest {

    @LocalServerPort
    int port;

    private record Reply(HttpStatusCode status, String body) {
    }

    private Reply post(String uri, String json) {
        return RestClient.create("http://localhost:" + port).post().uri(uri).contentType(MediaType.APPLICATION_JSON).body(json)
                .exchange((request, response) -> new Reply(response.getStatusCode(), new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)));
    }

    private Reply get(String uri) {
        return RestClient.create("http://localhost:" + port).get().uri(uri)
                .exchange((request, response) -> new Reply(response.getStatusCode(), new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Domyślnie: 400 i ogólny JSON Boota (status, error, path) — BEZ listy błędów pól")
    void defaultBodyOnRealServer() {
        Reply reply = post("/val02/products", Val02ValidInControllersTest.BAD);

        assertThat(reply.status().value()).isEqualTo(400);
        assertThat(reply.body()).contains("\"status\":400", "\"error\":\"Bad Request\"", "\"path\":\"/val02/products\"");
        assertThat(reply.body()).doesNotContain("Nazwa jest wymagana");   // klient nie dowiaduje się, CO jest źle
    }

    @Test
    @DisplayName("Z własnym @ExceptionHandler: 400 i mapa pole → komunikat")
    void friendlyBodyOnRealServer() {
        Reply reply = post("/val02/friendly/products", Val02ValidInControllersTest.BAD);

        assertThat(reply.status().value()).isEqualTo(400);
        assertThat(reply.body()).isEqualTo("{\"name\":\"Nazwa jest wymagana\",\"price\":\"Cena musi wynosić co najmniej 0.01\","
                + "\"quantity\":\"Ilość nie może być ujemna\"}");
    }

    @Test
    @DisplayName("Poprawne dane na prawdziwym serwerze: 201, a błąd parametru ścieżki: 400")
    void happyPathAndParameter() {
        assertThat(post("/val02/products", Val02ValidInControllersTest.GOOD).status().value()).isEqualTo(201);
        assertThat(get("/val02/products/0").status().value()).isEqualTo(400);
        assertThat(get("/val02/products?q=abcdefghijk").status().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("Kontroler ze starym @Validated na klasie: błąd parametru to na prawdziwym serwerze 500")
    void legacyValidatedIs500OnRealServer() {
        assertThat(get("/val02/legacy/products/0").status().value()).isEqualTo(500);
        assertThat(get("/val02/legacy/products/1").status().value()).isEqualTo(200);
    }
}
