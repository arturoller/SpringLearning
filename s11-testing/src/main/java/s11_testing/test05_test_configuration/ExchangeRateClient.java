package s11_testing.test05_test_configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * ExchangeRateClient = klient zewnętrznego serwisu kursów walut (np. bank): {@code GET {base-url}/rates/EUR} → tekst
 * „4.25”. Adres bierze z właściwości {@code shop.exchange.base-url}. Domyślny adres (port 9 na localhost) celowo
 * prowadzi donikąd — w testach NIGDY nie łączymy się z prawdziwym internetem; test podstawia adres swojego
 * lokalnego serwera-zaślepki przez {@code @DynamicPropertySource} (sekcja 4 lekcji).
 */
@Component
public class ExchangeRateClient {

    private final RestClient restClient;

    public ExchangeRateClient(@Value("${shop.exchange.base-url:http://localhost:9}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);   // RestClient = klient HTTP Springa (szczegóły: s13_rest_client)
    }

    public BigDecimal eurRate() {
        String body = restClient.get().uri("/rates/EUR").retrieve().body(String.class);
        return new BigDecimal(body.trim());
    }
}
