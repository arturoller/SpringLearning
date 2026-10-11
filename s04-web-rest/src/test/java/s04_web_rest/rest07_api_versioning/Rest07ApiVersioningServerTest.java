package s04_web_rest.rest07_api_versioning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.client.RestClient;
import s04_web_rest.rest07_api_versioning.VersionedModels.ProductV1;
import s04_web_rest.rest07_api_versioning.VersionedModels.ProductV2;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Rest07ApiVersioning: KLIENT też zna wersje. RestClient z ApiVersionInserter (wstawiacz wersji)
 * sam dopisuje nagłówek API-Version — w kodzie wołasz tylko .apiVersion("2.0"). Prawdziwy serwer na losowym porcie.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Rest07ApiVersioningServerTest {

    @LocalServerPort
    int port;

    @Test
    @DisplayName("5. RestClient.apiVersion(...) + ApiVersionInserter.useHeader: ten sam adres, dwie wersje odpowiedzi")
    void clientSendsVersion() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .apiVersionInserter(ApiVersionInserter.useHeader("API-Version"))
                .build();

        ProductV1 v1 = client.get().uri("/api/products/1").apiVersion("1.1").retrieve().body(ProductV1.class);
        ProductV2 v2 = client.get().uri("/api/products/1").apiVersion("2.0").retrieve().body(ProductV2.class);

        assertThat(v1).isEqualTo(new ProductV1(1, "Kawa", 2999));
        assertThat(v2).isNotNull();
        assertThat(v2.price().amount()).isEqualByComparingTo(new BigDecimal("29.99"));
        assertThat(v2.price().currency()).isEqualTo("PLN");
    }
}
