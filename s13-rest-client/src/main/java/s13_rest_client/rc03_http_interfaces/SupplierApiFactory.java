package s13_rest_client.rc03_http_interfaces;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * SupplierApiFactory = RĘCZNE tworzenie klienta-interfejsu (sposób z Spring Framework 6 / Boot 3, wciąż działa).
 * Dokładnie ten kod pisało się w metodzie {@code @Bean} — w Boot 4 robi to za Ciebie {@code @ImportHttpServices}.
 *
 * <ol>
 *   <li>RestClientAdapter (adapter) — „przejściówka”: pozwala fabryce wysyłać zapytania przez RestClient,</li>
 *   <li>HttpServiceProxyFactory (fabryka proxy usług HTTP) — czyta adnotacje interfejsu,</li>
 *   <li>createClient(SupplierApi.class) — tworzy obiekt pośredniczący (proxy) implementujący interfejs.</li>
 * </ol>
 */
public final class SupplierApiFactory {

    private SupplierApiFactory() {
    }

    /** @param restClient klient z adresem bazowym i nagłówkami domyślnymi (np. X-Shop-Id) */
    public static SupplierApi create(RestClient restClient) {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(SupplierApi.class);
    }
}
