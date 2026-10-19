package s04_web_rest.rest01_controllers;

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
 * Lekcja Rest01Controllers na PRAWDZIWYM serwerze (RANDOM_PORT = losowy wolny port). MockMvc nie wykonuje
 * przekazania (forward) ani nie renderuje strony błędu Boota (/error) — tu widać, co naprawdę dostaje klient.
 * Klient HTTP z JDK (java.net.http), bo niczego sam nie dodaje do zapytań.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Rest01ControllersRealServerTest {

    @LocalServerPort
    int port;

    private HttpResponse<String> send(String method, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("1. PUŁAPKA na prawdziwym serwerze: sam @Controller → forward pod „witaj” → 404 (pytanie 4)")
    void viewNameGives404OnRealServer() throws Exception {
        HttpResponse<String> response = send("GET", "/api/greeting/view");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).doesNotContain("witaj\"").contains("\"status\":404");
    }

    @Test
    @DisplayName("4. 405 i 404 na prawdziwym serwerze: nagłówek Allow i JSON strony błędu Boota (timestamp, status, error, path)")
    void errorsOnRealServer() throws Exception {
        HttpResponse<String> notAllowed = send("POST", "/api/greeting/rest");
        assertThat(notAllowed.statusCode()).isEqualTo(405);
        assertThat(notAllowed.headers().firstValue("Allow")).hasValue("GET");
        assertThat(notAllowed.body()).contains("\"error\":\"Method Not Allowed\"", "\"path\":\"/api/greeting/rest\"");

        HttpResponse<String> notFound = send("GET", "/api/nothing");
        assertThat(notFound.statusCode()).isEqualTo(404);
        assertThat(notFound.body()).contains("\"error\":\"Not Found\"", "\"path\":\"/api/nothing\"");
    }
}
