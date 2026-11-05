package s11_testing.test02_mockito_beans;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/**
 * Prawdziwy klient usługi cen: wysyła zapytanie HTTP (RestClient — s13_rest_client). W testach tego beana
 * NIE wolno wywoływać (żadnej sieci w testach!) — dlatego test podstawia atrapę {@code @MockitoBean PriceClient}.
 */
@Component
public class RemotePriceClient implements PriceClient {

    private final RestClient client;

    public RemotePriceClient(@Value("${test02.price-service.url:http://localhost:9}") String baseUrl) {
        this.client = RestClient.create(baseUrl);
    }

    @Override
    public BigDecimal currentPrice(String productName) {
        try {
            return client.get().uri("/prices?name={name}", productName).retrieve().body(BigDecimal.class);
        } catch (RestClientException e) {
            throw new PriceUnavailableException("Usługa cen nie odpowiada", e);
        }
    }
}
