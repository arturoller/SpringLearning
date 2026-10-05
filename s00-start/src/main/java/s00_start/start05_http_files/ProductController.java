package s00_start.start05_http_files;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * ProductController = kontroler produktów. Trzy punkty końcowe (endpointy) pod wspólnym adresem /api/products.
 * Dokładnie te zapytania są w pliku s00-start/http/start05_http_files.http. Szczegóły REST — w dziale s04_web_rest.
 */
@RestController
@RequestMapping("/api/products")   // request mapping = mapowanie zapytań: wspólny początek adresu dla wszystkich metod
public class ProductController {

    private final ProductStore store;

    public ProductController(ProductStore store) {
        this.store = store;
    }

    /** GET /api/products → 200 OK i lista produktów (JSON-owa tablica). */
    @GetMapping
    public List<Product> all() {
        return store.findAll();
    }

    /**
     * GET /api/products/2 → 200 OK i jeden produkt albo 404 Not Found, gdy takiego id nie ma.
     * {@code @PathVariable} (zmienna ze ścieżki) — liczba z adresu trafia do parametru id.
     * {@code ResponseEntity} (encja odpowiedzi) — odpowiedź razem z kodem statusu.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> one(@PathVariable long id) {
        return store.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST /api/products z JSON-em w ciele → 201 Created, nagłówek Location z adresem nowego produktu i sam produkt.
     * {@code @RequestBody} (ciało zapytania) — Jackson zamienia JSON z ciała na rekord NewProduct.
     * Pusta nazwa albo ujemna cena → 400 Bad Request (porządna walidacja — dział s05_validation).
     */
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody NewProduct newProduct) {
        if (newProduct.name() == null || newProduct.name().isBlank() || newProduct.priceGrosze() < 0) {
            return ResponseEntity.badRequest().build();
        }
        Product created = store.add(newProduct);
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }
}
