package s13_rest_client.rc02_errors_and_timeouts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * SupplierCallLoggingInterceptor = przechwytywacz (interceptor), który loguje KAŻDE wywołanie dostawcy:
 * „Dostawca: GET http://... → 404 NOT_FOUND” albo „Dostawca: GET http://... → błąd sieci: ...”.
 *
 * <p>ClientHttpRequestInterceptor (przechwytywacz zapytań klienta) owija wysłanie zapytania — jak filtr po stronie
 * serwera, tylko po stronie klienta. {@code execution.execute(...)} wysyła zapytanie dalej (do kolejnego
 * przechwytywacza albo do biblioteki HTTP) i zwraca odpowiedź. Dodajemy go w builderze: {@code requestInterceptor(...)}.</p>
 *
 * <p>Logujemy metodę, adres i status — NIE ciało i NIE nagłówki z hasłami/tokenami (dane wrażliwe w logach to wyciek).</p>
 */
public class SupplierCallLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SupplierCallLoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        try {
            ClientHttpResponse response = execution.execute(request, body);   // właściwe wysłanie zapytania
            log.info("Dostawca: {} {} → {}", request.getMethod(), request.getURI(), response.getStatusCode());
            return response;
        } catch (IOException ex) {
            log.warn("Dostawca: {} {} → błąd sieci: {}", request.getMethod(), request.getURI(), ex.toString());
            throw ex;   // nie połykamy wyjątku — RestClient zamieni go na ResourceAccessException
        }
    }
}
