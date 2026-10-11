package s04_web_rest.rest02_request_data;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import s04_web_rest.rest02_request_data.RequestModels.ClientInfo;
import s04_web_rest.rest02_request_data.RequestModels.NewOrder;
import s04_web_rest.rest02_request_data.RequestModels.OrderLine;
import s04_web_rest.rest02_request_data.RequestModels.OrderSummary;
import s04_web_rest.rest02_request_data.RequestModels.ProductRef;
import s04_web_rest.rest02_request_data.RequestModels.SearchCriteria;

import java.util.List;
import java.util.Optional;

/**
 * ProductQueryController = kontroler zapytań o produkty — wszystkie sekcje lekcji Rest02RequestData.
 * Każda metoda pokazuje inne źródło danych zapytania: ścieżkę, parametry, nagłówki i ciało.
 */
@RestController
public class ProductQueryController {

    /**
     * Sekcja 1. {@code @PathVariable} (zmienna ze ścieżki). {@code GET /api/products/42} → id = 42L.
     * Spring KONWERTUJE tekst „42” na long; „abc” się nie da → 400 Bad Request.
     */
    @GetMapping("/api/products/{id}")
    public ProductRef byId(@PathVariable long id) {
        return new ProductRef("?", id);
    }

    /**
     * Sekcja 1. Dwie zmienne w ścieżce; {@code @PathVariable("id")} — jawna nazwa, gdy parametr Javy nazywa się inaczej.
     * {@code GET /api/categories/kawa/products/7} → ProductRef("kawa", 7).
     */
    @GetMapping("/api/categories/{category}/products/{id}")
    public ProductRef inCategory(@PathVariable String category, @PathVariable("id") long productId) {
        return new ProductRef(category, productId);
    }

    /**
     * Sekcja 2. {@code @RequestParam} (parametr zapytania — część adresu po „?”).
     * {@code GET /api/search?name=kawa&maxPrice=3000&tag=bio&tag=fair} →
     * name wymagany (brak → 400), maxPrice opcjonalny (brak → null), minPrice jako Optional, page i size
     * z wartościami domyślnymi, tag może wystąpić wiele razy → lista.
     */
    @GetMapping("/api/search")
    public SearchCriteria search(@RequestParam String name,
                                 @RequestParam(required = false) Integer maxPrice,
                                 @RequestParam Optional<Integer> minPrice,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(name = "tag", required = false) List<String> tags) {
        return new SearchCriteria(name, maxPrice, minPrice.orElse(null), page, size, tags == null ? List.of() : tags);
    }

    /**
     * Sekcja 2, ZŁY PRZYKŁAD (pytanie kontrolne 3): parametr nieobowiązkowy typu PROSTEGO int.
     * Gdy go brak, Spring musiałby wstawić null — a int nie może być null. Co wtedy? Zobacz test sekcji 2.
     */
    @GetMapping("/api/search/limited")
    public int limited(@RequestParam(required = false) int limit) {
        return limit;
    }

    /**
     * Sekcja 3. {@code @RequestHeader} (nagłówek zapytania). X-Client-Id wymagany (brak → 400),
     * Accept-Language z wartością domyślną „pl”. Wielkość liter w nazwie nagłówka nie ma znaczenia.
     */
    @GetMapping("/api/client-info")
    public ClientInfo clientInfo(@RequestHeader("X-Client-Id") String clientId,
                                 @RequestHeader(name = "Accept-Language", defaultValue = "pl") String language) {
        return new ClientInfo(clientId, language);
    }

    /**
     * Sekcja 4. {@code @RequestBody} (ciało zapytania) — Jackson zamienia JSON na rekord NewOrder.
     * Wymaga nagłówka Content-Type: application/json (inaczej 415).
     */
    @PostMapping("/api/orders")
    public OrderSummary placeOrder(@RequestBody NewOrder order) {
        List<OrderLine> lines = order.lines() == null ? List.of() : order.lines();
        int totalQuantity = lines.stream().mapToInt(OrderLine::quantity).sum();
        return new OrderSummary(order.customer(), lines.size(), totalQuantity);
    }
}
