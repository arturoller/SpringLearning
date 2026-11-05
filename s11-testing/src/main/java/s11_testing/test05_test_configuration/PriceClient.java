package s11_testing.test05_test_configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Klient zewnętrznej usługi cen (prawdziwe HTTP przez {@code RestClient}). Adres usługi pochodzi z właściwości
 * {@code test05.price-service.url}. W teście nie ma prawdziwej usługi — test uruchamia własny mały serwer-atrapę na
 * WOLNYM porcie i podaje jego adres przez {@code @DynamicPropertySource} (sekcja 3 lekcji).
 * <p>
 * Usługa odpowiada na {@code GET /prices/{kod}} samą liczbą w treści, np. {@code 24.90}.
 */
@Component
public class PriceClient {

    private final RestClient http;
    private final String baseUrl;

    public PriceClient(@Value("${test05.price-service.url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.http = RestClient.create(baseUrl); // RestClient.Builder z autokonfiguracji wymaga spring-boot-restclient — tu go nie ma
    }

    public BigDecimal currentPrice(String productCode) {
        String body = http.get().uri("/prices/{code}", productCode).retrieve().body(String.class);
        return new BigDecimal(body.trim());
    }

    public String baseUrl() {
        return baseUrl;
    }
}
