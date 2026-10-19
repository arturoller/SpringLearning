package s06_error_handling.err01_default_errors;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Kontroler z CELOWO nieobsłużonymi błędami — lekcja pokazuje, co Spring Boot robi sam, gdy my nic nie robimy.
 */
@RestController
public class ProductController {

    private static final Map<String, Product> PRODUCTS = Map.of(
            "ELE-001", new Product("ELE-001", "Laptop Pro 14", new BigDecimal("5499.99")),
            "KSI-001", new Product("KSI-001", "Czysty kod", new BigDecimal("79.00")));

    /** GET /api/products/ELE-001 → 200. GET /api/products/KSI-999 → wyjątek, którego nikt nie obsługuje → 500. */
    @GetMapping("/api/products/{sku}")
    public Product one(@PathVariable String sku) {
        Product product = PRODUCTS.get(sku);
        if (product == null) {
            // Komunikat z wewnętrznymi szczegółami — klient NIE powinien go zobaczyć (patrz PUŁAPKA w lekcji).
            throw new IllegalStateException("Brak produktu " + sku + " w tabeli PRODUCTS (jdbc:h2:mem:sklep, użytkownik sa)");
        }
        return product;
    }

    /** POST /api/products z błędnymi danymi → 400 (walidacja), bo {@code @Valid} sprawdza rekord NewProduct. */
    @PostMapping("/api/products")
    public Product create(@Valid @RequestBody NewProduct newProduct) {
        return new Product("NOW-001", newProduct.name(), newProduct.price());
    }

    /** GET /api/orders/abc → 400, bo "abc" nie da się zamienić na liczbę (long). */
    @GetMapping("/api/orders/{id}")
    public String order(@PathVariable long id) {
        return "ZAM-" + id;
    }
}
