package s04_web_rest.rest05_crud_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import s04_web_rest.rest05_crud_api.CrudModels.Product;
import s04_web_rest.rest05_crud_api.CrudModels.ProductPatch;
import s04_web_rest.rest05_crud_api.CrudModels.ProductRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 5 lekcji Rest05CrudApi: pełny cykl życia produktu przez PRAWDZIWY serwer (losowy port) i RestClient —
 * dokładnie to, co robią zapytania z pliku s04-web-rest/http/rest05_crud_api.http.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Rest05CrudApiServerTest {

    @LocalServerPort
    int port;

    @Autowired
    ProductRepository repository;   // ten sam bean, którego używa serwer — do przywrócenia danych

    RestClient client;

    @BeforeEach
    void setUp() {
        repository.resetToSampleData();
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    @DisplayName("5. Cykl życia: POST → GET z Location → PUT → PATCH → DELETE → 404")
    void fullLifecycle() {
        // POST
        ResponseEntity<Product> created = client.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ProductRequest("Ciastka", 1299))
                .retrieve().toEntity(Product.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        URI location = created.getHeaders().getLocation();
        assertThat(location).hasToString("http://localhost:" + port + "/api/products/4");

        // GET pod adresem z Location
        assertThat(client.get().uri(location).retrieve().body(Product.class))
                .isEqualTo(new Product(4, "Ciastka", 1299));

        // PUT — zastąp całość
        Product replaced = client.put().uri(location).contentType(MediaType.APPLICATION_JSON)
                .body(new ProductRequest("Ciastka owsiane", 1399))
                .retrieve().body(Product.class);
        assertThat(replaced).isEqualTo(new Product(4, "Ciastka owsiane", 1399));

        // PATCH — zmień tylko cenę
        Product patched = client.patch().uri(location).contentType(MediaType.APPLICATION_JSON)
                .body(new ProductPatch(null, 1199))
                .retrieve().body(Product.class);
        assertThat(patched).isEqualTo(new Product(4, "Ciastka owsiane", 1199));

        // DELETE → 204, potem GET → 404
        assertThat(client.delete().uri(location).retrieve().toBodilessEntity().getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThatThrownBy(() -> client.get().uri(location).retrieve().toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }
}
