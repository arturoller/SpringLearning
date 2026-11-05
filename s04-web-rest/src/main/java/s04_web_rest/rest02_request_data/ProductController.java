package s04_web_rest.rest02_request_data;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/** Sekcje 1 i 2 lekcji: dane ze ŚCIEŻKI ({@code @PathVariable}) i z PARAMETRÓW ZAPYTANIA ({@code @RequestParam}). */
@RestController
@RequestMapping("/rest02/products")
public class ProductController {

    private final ProductCatalog catalog;

    public ProductController(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * {id} w ścieżce to zmienna (path variable = zmienna ścieżki). Spring bierze z adresu napis "3" i zamienia go
     * na Long. Nazwę zmiennej odczytuje z NAZWY PARAMETRU metody (id) — to działa dzięki kompilacji z -parameters.
     * Brak produktu → 404 (statusy dokładniej: Rest03Responses).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> byId(@PathVariable Long id) {
        return ResponseEntity.of(catalog.byId(id));
    }

    /** To samo, ale nazwa zmiennej podana jawnie — wtedy nazwa parametru Javy może być dowolna. */
    @GetMapping("/by-id/{productId}")
    public ResponseEntity<Product> byIdExplicitName(@PathVariable("productId") Long number) {
        return ResponseEntity.of(catalog.byId(number));
    }

    /**
     * Filtrowanie listy parametrami zapytania: {@code /rest02/products?category=napoje&maxPrice=2000&tag=bio&limit=5}.
     * Wszystkie parametry są opcjonalne, każdy na inny sposób:
     * <ul>
     *   <li>category — {@code required = false}: brak parametru → null;</li>
     *   <li>maxPrice — {@code Optional}: brak parametru → Optional.empty() (bez sprawdzania null);</li>
     *   <li>tag — lista: {@code ?tag=bio&tag=import} (powtórzony parametr) albo {@code ?tag=bio,import};</li>
     *   <li>limit — {@code defaultValue}: brak parametru → "10" zamienione na int (i parametr przestaje być wymagany).</li>
     * </ul>
     */
    @GetMapping
    public List<Product> list(@RequestParam(required = false) String category,
                              @RequestParam Optional<Long> maxPrice,
                              @RequestParam(required = false) List<String> tag,
                              @RequestParam(defaultValue = "10") int limit) {
        return catalog.all().stream()
                .filter(product -> category == null || product.category().equals(category))
                .filter(product -> maxPrice.map(max -> product.priceGrosze() <= max).orElse(true))
                .filter(product -> tag == null || product.tags().stream().anyMatch(tag::contains))
                .limit(limit)
                .toList();
    }

    /** Parametr WYMAGANY (domyślnie): brak {@code ?q=…} → 400 Bad Request. */
    @GetMapping("/search")
    public List<Product> search(@RequestParam String q) {
        return catalog.all().stream()
                .filter(product -> product.name().toLowerCase().contains(q.toLowerCase()))
                .toList();
    }
}
