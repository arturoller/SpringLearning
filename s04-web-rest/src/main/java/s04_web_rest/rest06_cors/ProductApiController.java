package s04_web_rest.rest06_cors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sekcje 2–3 lekcji Rest06Cors: API produktów objęte GLOBALNĄ regułą CORS z CorsConfig (/rest06/api/**).
 * Sam kontroler nic o CORS nie wie — i o to chodzi: zasady są w jednym miejscu.
 */
@RestController
@RequestMapping("/rest06/api/products")
public class ProductApiController {

    private static final List<Product> PRODUCTS = List.of(
            new Product(1, "Kawa", 2490),
            new Product(2, "Herbata", 1590));

    /** Lista + własny nagłówek X-Total-Count (ujawniony przeglądarce przez exposedHeaders w CorsConfig). */
    @GetMapping
    public ResponseEntity<List<Product>> list() {
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(PRODUCTS.size()))
                .body(PRODUCTS);
    }

    /** Zmiana produktu — PUT jest na liście dozwolonych metod CORS. */
    @PutMapping("/{id}")
    public Product update(@PathVariable long id, @RequestBody Product product) {
        return new Product(id, product.name(), product.priceGrosze());
    }

    /** Usunięcie — DELETE NIE jest na liście metod CORS, więc przeglądarka z innego pochodzenia go nie wyśle. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return ResponseEntity.noContent().build();
    }
}
