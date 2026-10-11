package s04_web_rest.rest01_controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Testy lekcji Rest01Controllers na PRAWDZIWYM serwerze ({@code RANDOM_PORT} = losowy port). Potrzebne, bo strona
 * błędu Boota (JSON z polami status, error, path) to funkcja serwera — MockMvc jej nie pokazuje.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Rest01ControllersServerTest {

    @LocalServerPort
    int port;

    RestClient client;

    @BeforeEach
    void createClient() {
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    @DisplayName("2. Zapomniany @ResponseBody na prawdziwym serwerze → 404, choć metoda się wykonała (pytanie 4)")
    void forgottenResponseBodyGives404() {
        assertThatThrownBy(() -> client.get().uri("/api/shop/forgotten").retrieve().toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    @Test
    @DisplayName("5. Nieznany adres → 404 i JSON błędu Boota z polami status, error i path")
    void unknownPathGivesBootErrorJson() {
        HttpClientErrorException e = (HttpClientErrorException) catchThrowable(
                () -> client.get().uri("/api/nothing").retrieve().toBodilessEntity());

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(e.getResponseBodyAsString())
                .contains("\"status\":404", "\"error\":\"Not Found\"", "\"path\":\"/api/nothing\"");
    }

    @Test
    @DisplayName("4. Zła metoda HTTP na prawdziwym serwerze → 405 i nagłówek Allow")
    void methodNotAllowedOnServer() {
        HttpClientErrorException e = (HttpClientErrorException) catchThrowable(
                () -> client.post().uri("/api/shop/info").retrieve().toBodilessEntity());

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(e.getResponseHeaders()).isNotNull();
        assertThat(e.getResponseHeaders().getAllow()).extracting(Object::toString).containsExactly("GET");
    }
}
