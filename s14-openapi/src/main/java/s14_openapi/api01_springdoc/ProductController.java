package s14_openapi.api01_springdoc;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ProductController = kontroler produktów BEZ ŻADNEJ adnotacji dokumentującej. Springdoc i tak opisze go w całości —
 * czyta zwykłe adnotacje Spring MVC ({@code @GetMapping}, {@code @PathVariable}, {@code @RequestBody}…) i typy w sygnaturach.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final List<Product> products = new ArrayList<>(List.of(
            new Product("ELE-001", "Laptop Pro 14", "ELEKTRONIKA", new BigDecimal("5499.99"), 7),
            new Product("SPO-001", "Kawa ziarnista 1kg", "SPOZYWCZE", new BigDecimal("64.99"), 120),
            new Product("KSI-001", "Czysty kod", "KSIAZKI", new BigDecimal("79.00"), 15)));

    /** GET /api/products?category=KSIAZKI — lista, opcjonalnie tylko z jednej kategorii. */
    @GetMapping
    public List<Product> all(@RequestParam(required = false) String category) {
        return products.stream()
                .filter(p -> category == null || p.category().equals(category))
                .toList();
    }

    /** GET /api/products/ELE-001 — jeden produkt albo 404. */
    @GetMapping("/{sku}")
    public ResponseEntity<Product> one(@PathVariable String sku) {
        Optional<Product> found = products.stream().filter(p -> p.sku().equals(sku)).findFirst();
        return found.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** POST /api/products — nowy produkt, odpowiedź 201 Created z nagłówkiem Location. */
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody NewProduct in) {
        Product created = new Product(in.sku(), in.name(), in.category(), in.price(), in.stock());
        products.add(created);
        return ResponseEntity.created(URI.create("/api/products/" + created.sku())).body(created);
    }

    /** DELETE /api/products/ELE-001 — usuwa produkt, odpowiedź 204 No Content (bez ciała). */
    @DeleteMapping("/{sku}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String sku) {
        products.removeIf(p -> p.sku().equals(sku));
    }
}
