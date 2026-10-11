package s04_web_rest.rest03_responses;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test lekcji Rest03Responses na prawdziwym serwerze (losowy port): nagłówek Location zawiera host i port, pod którym
 * aplikacja NAPRAWDĘ działa — tak działa ServletUriComponentsBuilder (pytanie 7).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Rest03ResponsesServerTest {

    @LocalServerPort
    int port;

    @Test
    @DisplayName("3. Location na prawdziwym serwerze = http://localhost:<losowy port>/api/products/{id}")
    void locationContainsRealPort() {
        RestClient client = RestClient.create("http://localhost:" + port);

        ResponseEntity<Product> response = client.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new NewProduct("Miód", 2499))
                .retrieve()
                .toEntity(Product.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://localhost:" + port + "/api/products/" + response.getBody().id());
    }
}
