package s00_start.start05_http_files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Start05HttpFiles — te same zapytania co w pliku s00-start/http/start05_http_files.http, ale wysyłane
 * automatycznie. {@code webEnvironment = RANDOM_PORT} — prawdziwy serwer Tomcat na losowym wolnym porcie.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Start05HttpFilesTest {

    @LocalServerPort
    int port;   // numer losowego portu, na którym wystartował serwer w tym teście

    RestClient client;

    @BeforeEach   // before each = przed każdym testem
    void createClient() {
        client = RestClient.create("http://localhost:" + port);   // odpowiednik @host z pliku .http
    }

    @Test
    @DisplayName("2. GET /api/products → 200 OK i lista z produktami startowymi (zapytanie 1)")
    void getAll() {
        ResponseEntity<List<Product>> response = client.get().uri("/api/products")
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {   // typ z generykiem: List<Product>
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(Product::name).contains("Kawa", "Herbata", "Czekolada");
    }

    @Test
    @DisplayName("2. GET /api/products/2 → 200 OK i Herbata (zapytanie 2)")
    void getOne() {
        Product product = client.get().uri("/api/products/2").retrieve().body(Product.class);

        assertThat(product).isEqualTo(new Product(2, "Herbata", 1599));
    }

    @Test
    @DisplayName("2. GET /api/products/999 → 404 Not Found; RestClient zgłasza to wyjątkiem (zapytanie 3)")
    void getMissing() {
        assertThatThrownBy(() -> client.get().uri("/api/products/999").retrieve().toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    @Test
    @DisplayName("3. POST z JSON-em → 201 Created, nagłówek Location i nowy produkt (zapytanie 4)")
    void create() {
        ResponseEntity<Product> response = client.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)                      // nagłówek Content-Type
                .body(new NewProduct("Ciastka", 1299))                        // ciało — RestClient zamieni na JSON
                .retrieve()
                .toEntity(Product.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Product created = response.getBody();
        assertThat(created).isNotNull();
        assertThat(created.name()).isEqualTo("Ciastka");
        assertThat(response.getHeaders().getLocation()).hasToString("/api/products/" + created.id());

        // Location prowadzi do nowego produktu — sprawdzamy kolejnym GET-em
        assertThat(client.get().uri("/api/products/" + created.id()).retrieve().body(Product.class)).isEqualTo(created);
    }

    @Test
    @DisplayName("3. POST nie jest idempotentny: dwa takie same zapytania tworzą dwa produkty o kolejnych id")
    void postTwiceCreatesTwo() {
        NewProduct same = new NewProduct("Woda", 199);
        Product first = client.post().uri("/api/products").contentType(MediaType.APPLICATION_JSON).body(same)
                .retrieve().body(Product.class);
        Product second = client.post().uri("/api/products").contentType(MediaType.APPLICATION_JSON).body(same)
                .retrieve().body(Product.class);

        assertThat(second.id()).isEqualTo(first.id() + 1);
    }

    @Test
    @DisplayName("3. POST z ujemną ceną → 400 Bad Request (zapytanie 5)")
    void createInvalid() {
        assertThatThrownBy(() -> client.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new NewProduct("Błąd", -5))
                .retrieve().toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    @DisplayName("3. JSON wysłany bez Content-Type: application/json → 415 Unsupported Media Type (pytanie 4)")
    void wrongContentType() {
        assertThatThrownBy(() -> client.post().uri("/api/products")
                .contentType(MediaType.TEXT_PLAIN)
                .body("{\"name\": \"Ciastka\", \"priceGrosze\": 1299}")
                .retrieve().toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.class)
                .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode())
                        .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }
}
