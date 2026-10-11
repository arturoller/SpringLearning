package s05_validation.val02_valid_in_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 3 lekcji Val02ValidInControllers na PRAWDZIWYM serwerze ({@code @SpringBootTest} z losowym portem): MockMvc nie
 * pokazuje, jak błąd zamienia się w status HTTP po drodze przez serwer, więc raz sprawdzamy to naprawdę.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Val02ValidInControllersServerTest {

    @LocalServerPort
    int port;   // los portu wybrany przez Springa — nigdy na sztywno 8080

    private int statusOf(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    @DisplayName("3. Na serwerze: zły parametr bez @Validated → 400, a z @Validated na klasie → 500")
    void statusOnRealServer() throws Exception {
        assertThat(statusOf("/plain/items?limit=0")).isEqualTo(400);
        assertThat(statusOf("/validated/items?limit=0")).isEqualTo(500);
        assertThat(statusOf("/validated/items?limit=3")).isEqualTo(200);
    }
}
