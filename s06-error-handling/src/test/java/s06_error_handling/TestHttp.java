package s06_error_handling;

import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Pomocnik testów dla działu s06: wysyła zapytanie HTTP na prawdziwy serwer (RANDOM_PORT) i zwraca status, nagłówki
 * i ciało BEZ zgłaszania wyjątku przy 4xx/5xx (RestClient rzuca wyjątek przy błędach, a my chcemy oglądać ciało błędu).
 * Używa klienta z JDK ({@code java.net.http}) — niczego sam nie dodaje do zapytania, więc widzimy dokładnie to, co wysłaliśmy.
 */
public final class TestHttp {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final JsonMapper MAPPER = JsonMapper.builder().build();   // Jackson 3: pakiet tools.jackson.*

    private TestHttp() {
    }

    /** Odpowiedź: kod statusu, nagłówki, ciało jako tekst. */
    public record Reply(int status, HttpHeaders headers, String body) {

        /** Wartość nagłówka Content-Type albo pusty tekst. */
        public String contentType() {
            return headers.firstValue("Content-Type").orElse("");
        }

        /** Ciało JSON jako mapa pole → wartość (w kolejności z odpowiedzi). */
        @SuppressWarnings("unchecked")
        public Map<String, Object> json() {
            return MAPPER.readValue(body, Map.class);
        }
    }

    public static Reply get(int port, String path, String accept) {
        return send("GET", port, path, accept, null, null);
    }

    public static Reply get(int port, String path) {
        return send("GET", port, path, "application/json", null, null);
    }

    public static Reply postJson(int port, String path, String json) {
        return send("POST", port, path, "application/json", "application/json", json);
    }

    public static Reply send(String method, int port, String path, String accept, String contentType, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (accept != null) {
            builder.header("Accept", accept);
        }
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        try {
            HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.headers(), response.body());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Zapytanie nie powiodło się: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Przerwano zapytanie", e);
        }
    }
}
