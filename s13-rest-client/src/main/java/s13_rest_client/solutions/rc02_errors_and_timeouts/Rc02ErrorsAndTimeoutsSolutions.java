package s13_rest_client.solutions.rc02_errors_and_timeouts;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import s13_rest_client.rc02_errors_and_timeouts.ProductNotFoundAtSupplierException;
import s13_rest_client.rc02_errors_and_timeouts.StockLevel;
import s13_rest_client.rc02_errors_and_timeouts.SupplierUnavailableException;

import java.time.Duration;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rc02ErrorsAndTimeouts — zajrzyj dopiero po własnej próbie! */
public final class Rc02ErrorsAndTimeoutsSolutions {

    private Rc02ErrorsAndTimeoutsSolutions() {
    }

    static int solution1(RestClient client, String sku) {
        try {
            return client.get()
                    .uri("/supplier/api/stock/{sku}", sku)
                    .retrieve()
                    .requiredBody(StockLevel.class)
                    .available();
        } catch (HttpClientErrorException.NotFound e) {
            return -1;
        }
    }

    static String solution2(RestClientException exception) {
        return switch (exception) {
            case HttpClientErrorException.NotFound notFound -> "brak produktu";   // podklasa PRZED klasą bazową
            case HttpClientErrorException e -> "błąd zapytania: " + e.getStatusCode().value();
            case HttpServerErrorException e -> "awaria dostawcy: " + e.getStatusCode().value();
            case ResourceAccessException e -> "brak połączenia albo przekroczony czas";
            default -> "inny błąd";
        };
    }

    static StockLevel solution3(RestClient client, String sku) {
        return client.get()
                .uri("/supplier/api/stock/{sku}", sku)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                    throw new ProductNotFoundAtSupplierException(sku);
                })
                .onStatus(status -> status.isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE), (request, response) -> {
                    String retryAfter = response.getHeaders().getFirst("Retry-After");
                    throw new SupplierUnavailableException("Przerwa u dostawcy, spróbuj za " + retryAfter + " s");
                })
                .requiredBody(StockLevel.class);
    }

    static RestClient solution4(String baseUrl, Duration readTimeout) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk()
                        .build(HttpClientSettings.defaults().withReadTimeout(readTimeout)))
                .build();
    }
}
