package s11_testing.test03_mock_mvc_tester;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;

/**
 * Sekcja 6 lekcji Test03MockMvcTester: {@code RestTestClient} (Spring Framework 7) — klient testowy, który przy
 * {@code RANDOM_PORT} wysyła PRAWDZIWE zapytania HTTP do uruchomionego serwera. Serwis jest prawdziwy (dane
 * startowe katalogu), więc testy tylko CZYTAJĄ — nie zmieniają stanu współdzielonego kontekstu.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient   // Boot 4: tworzy bean RestTestClient skierowany na port uruchomionego serwera
class Test03MockMvcTesterRestTestClientTest {

    @Autowired
    RestTestClient client;

    @Test
    @DisplayName("6. RestTestClient: prawdziwe HTTP, odpowiedź zamieniona na rekord")
    void getProductOverRealHttp() {
        client.get().uri("/api/products/{sku}", "KSI-001")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponse.class)
                .isEqualTo(new ProductResponse("KSI-001", "Czysty kod", "KSIAZKI", new BigDecimal("79.00")));
    }

    @Test
    @DisplayName("6. Prawdziwy serwer: 404 ma ciało JSON ze strony /error Boota (MockMvc go nie pokazuje)")
    void notFoundHasErrorBodyOnRealServer() {
        client.get().uri("/api/products/XXX-999")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.error").isEqualTo("Not Found")
                .jsonPath("$.path").isEqualTo("/api/products/XXX-999");
    }
}
