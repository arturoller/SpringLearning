package s13_rest_client.rc01_rest_client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * SupplierClient = klient API dostawcy: jedyne miejsce w sklepie, które wie, JAK rozmawiać z dostawcą
 * (adres, nagłówki, ścieżki, format JSON). Reszta aplikacji woła zwykłe metody Javy: {@code offer("ELE-001")}.
 *
 * <p>RestClient (klient REST, od Spring Framework 6.1) budujemy RAZ, w konstruktorze, z buildera (budowniczego)
 * dostarczonego przez Spring Boota, i używamy go w każdej metodzie.</p>
 */
@Component
public class SupplierClient {

    private final RestClient restClient;

    /**
     * @param builder RestClient.Builder od Boota — bean o zasięgu prototype (każde wstrzyknięcie = NOWY builder),
     *                już skonfigurowany: konwertery JSON (Jackson 3), biblioteka HTTP, limity czasu z właściwości
     * @param baseUrl adres bazowy API dostawcy (base URL = adres bazowy) — z właściwości {@code rc01.supplier.base-url}
     * @param shopId  identyfikator naszego sklepu u dostawcy — wysyłany w KAŻDYM zapytaniu jako nagłówek X-Shop-Id
     */
    public SupplierClient(RestClient.Builder builder,
                          @Value("${rc01.supplier.base-url}") String baseUrl,
                          @Value("${rc01.supplier.shop-id}") String shopId) {
        this.restClient = builder
                .baseUrl(baseUrl)                                                     // początek każdego adresu
                .defaultHeader(FakeSupplierController.SHOP_ID_HEADER, shopId)         // nagłówek w każdym zapytaniu
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)  // „odpowiadaj mi JSON-em”
                .build();
    }

    /**
     * GET /supplier/api/offers/{sku} → jedna oferta. {@code {sku}} to zmienna szablonu adresu (URI template):
     * RestClient wstawia w nią wartość i KODUJE ją (spacja → %20), więc nie sklejaj adresów plusem.
     * {@code requiredBody} (wymagane ciało, od Spring Framework 7.0.4) — gdy odpowiedź nie ma ciała, rzuca
     * IllegalStateException zamiast zwracać null.
     */
    public SupplierOffer offer(String sku) {
        return restClient.get()
                .uri("/supplier/api/offers/{sku}", sku)
                .retrieve()                                  // retrieve = pobierz (jeszcze NIC nie wysyła!)
                .requiredBody(SupplierOffer.class);          // dopiero teraz: wysłanie, odczyt, JSON → rekord
    }

    /**
     * To samo zapytanie, ale z całą odpowiedzią: {@code ResponseEntity} (encja odpowiedzi) = status + nagłówki + ciało.
     * Przydaje się, gdy ważne są nagłówki (tu: Cache-Control od dostawcy).
     */
    public ResponseEntity<SupplierOffer> offerWithHeaders(String sku) {
        return restClient.get()
                .uri("/supplier/api/offers/{sku}", sku)
                .retrieve()
                .toEntity(SupplierOffer.class);
    }

    /**
     * GET /supplier/api/offers?category=… → LISTA ofert. Parametr zapytania (query param) dokłada UriBuilder.
     * Typ {@code List<SupplierOffer>} podajemy przez ParameterizedTypeReference (odwołanie do typu z parametrem):
     * samo {@code List.class} nie niesie informacji „lista CZEGO” (wymazywanie typów — t11_generics/Generics06ErasureLimits).
     */
    public List<SupplierOffer> offersInCategory(String category) {
        List<SupplierOffer> offers = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/supplier/api/offers").queryParam("category", category).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<SupplierOffer>>() {   // anonimowa podklasa „zapamiętuje” typ
                });
        return offers == null ? List.of() : offers;   // body(...) może zwrócić null, gdy odpowiedź nie ma ciała
    }

    /**
     * POST /supplier/api/orders z JSON-em w ciele → 201 Created + Location + potwierdzenie.
     * {@code body(obiekt)} — RestClient zamienia rekord na JSON (Jackson), {@code contentType} ustawia nagłówek
     * Content-Type: application/json, żeby serwer wiedział, jak czytać ciało.
     */
    public ResponseEntity<SupplierOrderConfirmation> placeOrder(String sku, int quantity) {
        return restClient.post()
                .uri("/supplier/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SupplierOrder(sku, quantity))
                .retrieve()
                .toEntity(SupplierOrderConfirmation.class);
    }
}
