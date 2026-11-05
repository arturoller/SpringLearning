package s04_web_rest.rest05_crud_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rest05CrudApi — PRAWDZIWY serwer. {@code @SpringBootTest(webEnvironment = RANDOM_PORT)} uruchamia całą
 * aplikację z Tomcatem na LOSOWYM wolnym porcie (nigdy sztywne 8080 — port mógłby być zajęty), a {@code RestClient}
 * wysyła zapytania prawdziwym HTTP przez sieć lokalną. Droższe niż MockMvc, ale pokazuje wszystko razem:
 * kontroler, serwis, JSON, nagłówki. Jeden taki test na dział wystarczy.
 * <p>
 * W Boot 4 jest też {@code TestRestTemplate} (pakiet {@code org.springframework.boot.resttestclient}, wymaga adnotacji
 * {@code @AutoConfigureTestRestTemplate}); my używamy {@code RestClient}, bo to ten sam klient, którego używa się w
 * kodzie produkcyjnym (dział s13_rest_client).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Rest05ServerTest {

    @LocalServerPort
    int port;                 // port wybrany przez system

    @Autowired
    ProductService service;   // prawdziwy serwis z aplikacji — do przywrócenia stanu

    RestClient client;

    @BeforeEach
    void setUp() {
        service.reset();
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    @DisplayName("Pełny scenariusz CRUD przez prawdziwy HTTP: POST → GET → PUT → PATCH → DELETE → GET (404)")
    void fullScenario() {
        // C — create
        ResponseEntity<ProductResponse> created = client.post().uri("/rest05/products")
                .body(new ProductRequest("Ciastka", new BigDecimal("12.50")))
                .retrieve().toEntity(ProductResponse.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getHeaders().getLocation()).hasToString("http://localhost:" + port + "/rest05/products/3");
        assertThat(created.getBody()).isEqualTo(new ProductResponse(3, "Ciastka", new BigDecimal("12.50")));

        // R — read (pod adresem z Location)
        ProductResponse read = client.get().uri(created.getHeaders().getLocation()).retrieve().body(ProductResponse.class);
        assertThat(read).isEqualTo(created.getBody());

        List<ProductResponse> all = client.get().uri("/rest05/products").retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() { });
        assertThat(all).extracting(ProductResponse::id).containsExactly(1L, 2L, 3L);

        // U — update (PUT = całość, PATCH = fragment)
        ProductResponse replaced = client.put().uri("/rest05/products/3")
                .body(new ProductRequest("Ciastka maślane", new BigDecimal("14.00")))
                .retrieve().body(ProductResponse.class);
        assertThat(replaced.name()).isEqualTo("Ciastka maślane");

        ProductResponse patched = client.patch().uri("/rest05/products/3")
                .body(new PricePatch(new BigDecimal("9.99")))
                .retrieve().body(ProductResponse.class);
        assertThat(patched.name()).isEqualTo("Ciastka maślane");           // nazwa bez zmian
        assertThat(patched.price()).isEqualByComparingTo("9.99");

        // D — delete, a potem 404
        ResponseEntity<Void> deleted = client.delete().uri("/rest05/products/3").retrieve().toBodilessEntity();
        assertThat(deleted.getStatusCode().value()).isEqualTo(204);

        assertThatThrownBy(() -> client.get().uri("/rest05/products/3").retrieve().toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    @Test
    @DisplayName("RestClient zamienia odpowiedzi 4xx na wyjątek (HttpClientErrorException) — tu 400 dla złych danych")
    void clientErrorsAreExceptions() {
        assertThatThrownBy(() -> client.post().uri("/rest05/products")
                .body(new ProductRequest(" ", new BigDecimal("1")))
                .retrieve().toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }
}
