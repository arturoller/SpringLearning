package s13_rest_client.rc03_http_interfaces;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Optional;

/**
 * SupplierApi = API dostawcy opisane INTERFEJSEM (HTTP interface — interfejs HTTP, „deklaratywny klient”).
 * Nie piszesz implementacji: Spring tworzy obiekt pośredniczący (proxy), który z adnotacji składa zapytanie HTTP
 * i wysyła je przez RestClient. Adnotacje są lustrem tych z kontrolera: {@code @GetMapping} → {@code @GetExchange}.
 *
 * <p>{@code @HttpExchange} na interfejsie — wspólny początek ścieżki i nagłówek Accept dla wszystkich metod.
 * Adres serwera (http://...) NIE jest tutaj — daje go RestClient (u nas: właściwość grupy {@code dostawca}).</p>
 */
@HttpExchange(url = "/supplier/api", accept = MediaType.APPLICATION_JSON_VALUE)
public interface SupplierApi {

    /** GET /supplier/api/offers/{sku} → oferta; 404 → HttpClientErrorException.NotFound (jak w RestClient). */
    @GetExchange("/offers/{sku}")
    SupplierOffer offer(@PathVariable String sku);

    /** To samo zapytanie, ale z całą odpowiedzią (status, nagłówki, ciało). */
    @GetExchange("/offers/{sku}")
    ResponseEntity<SupplierOffer> offerEntity(@PathVariable String sku);

    /**
     * To samo zapytanie, wynik w Optional. Optional.empty() dostaniesz, gdy odpowiedź nie ma ciała — a 404 domyślnie
     * i tak jest WYJĄTKIEM. Pusty Optional dla 404 daje dopiero NotFoundRestClientAdapterDecorator (sekcja 4).
     */
    @GetExchange("/offers/{sku}")
    Optional<SupplierOffer> findOffer(@PathVariable String sku);

    /** GET /supplier/api/offers?category=… → lista; typ z generykiem odczytany z sygnatury metody (bez ParameterizedTypeReference). */
    @GetExchange("/offers")
    List<SupplierOffer> offers(@RequestParam String category);

    /** POST /supplier/api/orders z JSON-em w ciele → potwierdzenie. */
    @PostExchange(url = "/orders", contentType = MediaType.APPLICATION_JSON_VALUE)
    SupplierOrderConfirmation order(@RequestBody SupplierOrder order);
}
