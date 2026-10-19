package s12_security.sec02_authorization;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

/**
 * ProductController = produkty: ten sam adres, różne metody HTTP — różne reguły (sekcja 3 lekcji).
 * Zapis jest udawany (nic nie zapisujemy) — lekcja dotyczy tylko tego, KTO może wywołać metodę.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    /** Produkt (ceny w złotych jako BigDecimal). */
    public record Product(String sku, String name, BigDecimal price) {
    }

    private static final List<Product> PRODUCTS = List.of(
            new Product("ELE-001", "Laptop Pro 14", new BigDecimal("5499.99")),
            new Product("SPO-001", "Kawa ziarnista 1kg", new BigDecimal("64.99")),
            new Product("KSI-001", "Czysty kod", new BigDecimal("79.00")));

    /** GET — wszyscy. */
    @GetMapping
    public List<Product> all() {
        return PRODUCTS;
    }

    /** POST — tylko ADMIN. Odpowiedź 201 Created z adresem „nowego” produktu. */
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        return ResponseEntity.created(URI.create("/api/products/" + product.sku())).body(product);
    }

    /** DELETE — tylko ADMIN. Odpowiedź 204 No Content (brak treści). */
    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> delete(@PathVariable String sku) {
        return ResponseEntity.noContent().build();
    }
}
