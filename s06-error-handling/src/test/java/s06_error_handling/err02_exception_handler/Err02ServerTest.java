package s06_error_handling.err02_exception_handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Obie strony medalu na PRAWDZIWYM serwerze: kontroler z handlerem odpowiada własnym ciałem {@code ApiError}, a kontroler
 * bez handlera — domyślnym 500 z {@code /error} (Err01DefaultErrors).
 */
@SpringBootTest(classes = Err02Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Err02ServerTest {

    @LocalServerPort
    int port;

    private record Reply(HttpStatusCode status, String body) {
    }

    private Reply get(String uri) {
        return RestClient.create("http://localhost:" + port).get().uri(uri).header("Accept", "application/json")
                .exchange((req, response) -> new Reply(response.getStatusCode(), new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Kontroler z handlerem: 404 i ciało ApiError (status, code, message, path)")
    void controllerWithHandler() {
        Reply reply = get("/err02/shop/products/9");

        assertThat(reply.status().value()).isEqualTo(404);
        assertThat(reply.body()).isEqualTo("{\"status\":404,\"code\":\"PRODUCT_NOT_FOUND\",\"message\":\"Nie ma produktu o id 9\","
                + "\"path\":\"/err02/shop/products/9\"}");
    }

    @Test
    @DisplayName("Kontroler bez handlera: ten sam wyjątek daje 500 i ogólny JSON Boota (bez code i message)")
    void controllerWithoutHandler() {
        Reply reply = get("/err02/other/products/9");

        assertThat(reply.status().value()).isEqualTo(500);
        assertThat(reply.body()).contains("\"status\":500", "\"error\":\"Internal Server Error\"").doesNotContain("PRODUCT_NOT_FOUND", "Nie ma produktu");
    }

    @Test
    @DisplayName("Wyjątek rzucony w handlerze: na serwerze klient dostaje 500 z /error, a nie pierwotny 404/500 z handlera")
    void brokenHandlerOnRealServer() {
        Reply reply = get("/err02/shop/products/3");

        assertThat(reply.status().value()).isEqualTo(500);
        assertThat(reply.body()).contains("\"error\":\"Internal Server Error\"").doesNotContain("\"code\"");
    }
}
