package s04_web_rest.rest07_api_versioning;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sekcje 2–4 lekcji Rest07ApiVersioning: TEN SAM adres, różne wersje API → różne metody Javy.
 * Wersję wybiera atrybut {@code version} mapowania (nowość Spring Framework 7). SKĄD wziąć wersję z zapytania
 * (nagłówek, parametr, ścieżka), decyduje konfiguracja — nie kontroler.
 */
@RestController
@RequestMapping("/rest07/products")
public class ProductController {

    private final ProductCatalog catalog;

    public ProductController(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    /** Wersja 1.0: cena w groszach. */
    @GetMapping(path = "/{id}", version = "1.0")
    public ResponseEntity<ProductV1> byIdV1(@PathVariable long id) {
        return ResponseEntity.of(catalog.find(id).map(catalog::toV1));
    }

    /** Wersja 2.0: cena jako obiekt {amount, currency}. */
    @GetMapping(path = "/{id}", version = "2.0")
    public ResponseEntity<ProductV2> byIdV2(@PathVariable long id) {
        return ResponseEntity.of(catalog.find(id).map(catalog::toV2));
    }

    /**
     * Wersja „1.1+” = BASELINE (wersja bazowa): endpoint dodany w 1.1 i dostępny w każdej NOWSZEJ wersji (1.1, 2.0 …),
     * bez kopiowania metody do każdej wersji.
     */
    @GetMapping(path = "/{id}/stock", version = "1.1+")
    public ResponseEntity<Map<String, Object>> stock(@PathVariable long id) {
        return ResponseEntity.of(catalog.find(id).map(item -> Map.of("id", item.id(), "stock", item.stock())));
    }

    /** Mapowanie BEZ atrybutu version — pasuje do zapytania w każdej wersji. */
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
