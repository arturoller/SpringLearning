package s13_rest_client.rc02_errors_and_timeouts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * SupplierStockClient = klient „stanu magazynu” dostawcy. Pokazuje trzy sposoby obsługi błędów HTTP:
 * domyślne wyjątki Springa, {@code onStatus} z wyjątkami domenowymi i {@code exchange} z pełną kontrolą.
 * Każde wywołanie loguje SupplierCallLoggingInterceptor.
 */
@Component
public class SupplierStockClient {

    private final RestClient restClient;

    public SupplierStockClient(RestClient.Builder builder, @Value("${rc02.supplier.base-url}") String baseUrl) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .requestInterceptor(new SupplierCallLoggingInterceptor())   // logowanie każdego wywołania
                .build();
    }

    /**
     * Sekcja 1 — zachowanie DOMYŚLNE: 4xx → HttpClientErrorException, 5xx → HttpServerErrorException,
     * brak odpowiedzi (sieć, limit czasu) → ResourceAccessException. Wyjątki z pakietu HTTP wyciekają do wołającego.
     */
    public StockLevel stockWithDefaultErrors(String sku) {
        return restClient.get()
                .uri("/supplier/api/stock/{sku}", sku)
                .retrieve()
                .requiredBody(StockLevel.class);
    }

    /**
     * Sekcja 2 — {@code onStatus} (przy statusie): własna reakcja na wybrane statusy, zanim zadziała domyślna.
     * 404 → ProductNotFoundAtSupplierException, 5xx → SupplierUnavailableException. Błąd SIECI nie jest statusem
     * (odpowiedzi nie ma), więc onStatus go nie zobaczy — łapiemy ResourceAccessException osobno.
     */
    public StockLevel stock(String sku) {
        try {
            return restClient.get()
                    .uri("/supplier/api/stock/{sku}", sku)
                    .retrieve()
                    .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                        throw new ProductNotFoundAtSupplierException(sku);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new SupplierUnavailableException("Dostawca odpowiedział " + response.getStatusCode());
                    })
                    .requiredBody(StockLevel.class);
        } catch (ResourceAccessException ex) {
            throw new SupplierUnavailableException("Brak odpowiedzi od dostawcy", ex);
        }
    }

    /**
     * Sekcja 3 — {@code exchange} (wymiana): TY decydujesz, co zrobić z każdą odpowiedzią; Spring nie rzuca
     * żadnego wyjątku za status. 200 → Optional z wynikiem, 404 → Optional.empty() („nie ma” to normalna odpowiedź,
     * nie błąd), inny status → wyjątek utworzony przez {@code createException()} (nowość Spring Framework 7.0).
     */
    public Optional<StockLevel> findStock(String sku) {
        return restClient.get()
                .uri("/supplier/api/stock/{sku}", sku)
                .exchangeForRequiredValue((request, response) -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        return Optional.ofNullable(response.bodyTo(StockLevel.class));   // sami czytamy ciało
                    }
                    if (response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                        return Optional.empty();
                    }
                    throw response.createException();   // HttpServerErrorException itd. — jak przy retrieve()
                });
    }
}
