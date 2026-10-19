package s13_rest_client.rc04_resilience;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * SupplierClient = klient „kapryśnego” dostawcy z odpornością Spring Framework 7:
 * {@code @Retryable} (ponów przy awarii przejściowej) i {@code @ConcurrencyLimit} (najwyżej N wywołań naraz).
 *
 * <p>Obie adnotacje działają przez PROXY (obiekt pośredniczący) — tylko gdy aplikacja ma {@code @EnableResilientMethods}
 * (Rc04Application) i tylko przy wywołaniu z ZEWNĄTRZ beana. Parametry ponawiania są we właściwościach
 * (application-rc04.properties), a testy ustawiają opóźnienie 0 ms — testy nie czekają i nie zależą od zegara.</p>
 */
@Component
public class SupplierClient {

    private final RestClient restClient;

    public SupplierClient(RestClient.Builder builder, @Value("${rc04.supplier.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * GET ceny — operacja IDEMPOTENTNA (powtórzenie niczego nie zmienia), więc można ją bezpiecznie ponawiać.
     * Ponawiamy tylko awarie przejściowe: 5xx (HttpServerErrorException) i sieć/limit czasu (ResourceAccessException).
     * 404 (HttpClientErrorException) nie ponawiamy — od powtarzania produkt się nie pojawi.
     * maxRetries = 3 → najwyżej 1 + 3 = 4 próby; opóźnienia: delay, potem razy multiplier (200 ms, 400 ms, 800 ms),
     * ale nie więcej niż maxDelay.
     */
    @Retryable(includes = {HttpServerErrorException.class, ResourceAccessException.class},
            maxRetriesString = "${rc04.retry.max-retries}",
            delayString = "${rc04.retry.delay}",
            multiplier = 2,
            maxDelayString = "${rc04.retry.max-delay}")
    public SupplierPrice price(String sku) {
        return restClient.get()
                .uri("/supplier/api/prices/{sku}", sku)
                .retrieve()
                .requiredBody(SupplierPrice.class);
    }

    /**
     * PUŁAPKA: celowo zły kod — nie naśladuj! Ceny koszyka liczone przez wywołanie {@code price(...)} z TEJ SAMEJ
     * klasy. To wywołanie {@code this.price(...)} omija proxy, więc @Retryable NIE działa — pierwsze 503 kończy całość.
     */
    public Map<String, BigDecimal> basketPrices(List<String> skus) {
        Map<String, BigDecimal> result = new TreeMap<>();
        for (String sku : skus) {
            result.put(sku, price(sku).price());   // this.price(...) — z pominięciem proxy
        }
        return result;
    }

    /**
     * PUŁAPKA: celowo zły kod — nie naśladuj! Ponawianie POST-a, który NIE jest idempotentny. Gdy dostawca zapisał
     * zamówienie, a odpowiedź „zginęła” (503, limit czasu), ponowienie zapisze DRUGIE zamówienie.
     */
    @Retryable(includes = {HttpServerErrorException.class, ResourceAccessException.class},
            maxRetriesString = "${rc04.retry.max-retries}",
            delayString = "${rc04.retry.delay}")
    public SupplierOrderConfirmation placeOrderUnsafe(SupplierOrder order) {
        return restClient.post()
                .uri("/supplier/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(order)
                .retrieve()
                .requiredBody(SupplierOrderConfirmation.class);
    }

    /**
     * DOBRZE: POST z kluczem idempotencji (Idempotency-Key). Klucz przychodzi OD WOŁAJĄCEGO, więc każda próba
     * wysyła TEN SAM klucz — dostawca rozpoznaje powtórkę i nie zapisuje zamówienia drugi raz.
     * (Klucz wygenerowany wewnątrz tej metody byłby inny w każdej próbie — i bezużyteczny.)
     */
    @Retryable(includes = {HttpServerErrorException.class, ResourceAccessException.class},
            maxRetriesString = "${rc04.retry.max-retries}",
            delayString = "${rc04.retry.delay}")
    public SupplierOrderConfirmation placeOrder(SupplierOrder order, String idempotencyKey) {
        return restClient.post()
                .uri("/supplier/api/orders")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(order)
                .retrieve()
                .requiredBody(SupplierOrderConfirmation.class);
    }

    /**
     * Raport magazynowy — „ciężkie” zapytanie: dostawca prosi o najwyżej 2 równoczesne. {@code @ConcurrencyLimit(2)}:
     * trzecie równoczesne wywołanie CZEKA (domyślna polityka BLOCK), aż któreś z dwóch się skończy.
     */
    @ConcurrencyLimit(2)
    public StockReport stockReport() {
        return restClient.get()
                .uri("/supplier/api/report")
                .retrieve()
                .requiredBody(StockReport.class);
    }
}
