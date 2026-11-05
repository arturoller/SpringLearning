package s12_security.sec06_security_testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Sec06SecurityTesting: test "od końca do końca" na prawdziwym serwerze (RANDOM_PORT) — bez żadnych atrap
 * bezpieczeństwa: prawdziwe hasło, prawdziwy token, prawdziwe filtry i dekoder. RestClient (TestRestTemplate nie jest dostępny
 * w tym module) + {@code @LocalServerPort}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Sec06ServerTest {

    @LocalServerPort
    int port;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private String tokenFor(String username) {
        JsonNode body = client().post().uri("/sec06/token")
                .headers(headers -> headers.setBasicAuth(username, "haslo-testowe"))
                .retrieve().body(JsonNode.class);
        return body.get("access_token").asString();
    }

    private int status(String uri, String token) {
        return client().get().uri(uri)
                .headers(headers -> {
                    if (token != null) {
                        headers.setBearerAuth(token);       // Authorization: Bearer <token>
                    }
                })
                .exchange((request, response) -> response.getStatusCode().value());
    }

    @Test
    @DisplayName("Sekcja 5: hasło → POST /sec06/token → Bearer → API; ewa widzi zamówienia piotra, anna — 403, bez tokenu 401")
    void fullFlowWithRealToken() {
        String ewa = tokenFor("ewa");
        String body = client().get().uri("/sec06/orders/of/piotr")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ewa)
                .retrieve().body(String.class);
        assertThat(body).contains("\"owner\":\"piotr\"");

        assertThat(status("/sec06/orders/of/piotr", tokenFor("anna"))).isEqualTo(403);
        assertThat(status("/sec06/orders/of/piotr", null)).isEqualTo(401);
        assertThat(status("/sec06/admin/stats", tokenFor("admin"))).isEqualTo(200);
    }
}
