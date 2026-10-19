package s16_cache_scheduling_async.cache01_cacheable;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Punkt końcowy (endpoint) do ręcznego sprawdzenia cache z pliku .http. */
@RestController
public class ProductController {

    private final ProductLookup lookup;

    public ProductController(ProductLookup lookup) {
        this.lookup = lookup;
    }

    @GetMapping("/api/products/{sku}")
    public ResponseEntity<Product> product(@PathVariable String sku) {
        Product product = lookup.findBySku(sku);
        return product == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(product);
    }

    /** Ile razy prawdziwa metoda findBySku została wykonana — po kilku zapytaniach o ten sam sku licznik stoi. */
    @GetMapping("/api/stats")
    public Map<String, Integer> stats() {
        return Map.of("findBySkuCalls", lookup.calls("findBySku"));
    }
}
