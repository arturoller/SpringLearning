package s11_testing.test03_mock_mvc_tester;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Kontroler produktów — obiekt testów lekcji Test03MockMvcTester. Szczegóły kontrolerów REST: dział s04_web_rest.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductCatalogService service;

    public ProductController(ProductCatalogService service) {
        this.service = service;
    }

    /** GET /api/products?category=KSIAZKI — lista (opcjonalnie z jednej kategorii). */
    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) String category) {
        return service.findAll(category);
    }

    /** GET /api/products/{sku} — 200 z produktem albo 404 (ResponseStatusException). */
    @GetMapping("/{sku}")
    public ProductResponse bySku(@PathVariable String sku) {
        return service.findBySku(sku)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma produktu " + sku));
    }

    /** POST /api/products — 201 Created + nagłówek Location + utworzony produkt. */
    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody NewProductRequest request) {
        ProductResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + created.sku())).body(created);
    }

    /** DELETE /api/products/{sku} — 204 No Content albo 404. */
    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> delete(@PathVariable String sku) {
        return service.delete(sku) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * GET /api/products/{sku}/reviews — funkcja „w budowie”: rzuca wyjątek, którego NIKT nie obsługuje.
     * Służy do pokazania, jak MockMvc i MockMvcTester zachowują się przy nieobsłużonym wyjątku (sekcja 5).
     */
    @GetMapping("/{sku}/reviews")
    public List<String> reviews(@PathVariable String sku) {
        throw new IllegalStateException("Opinie o produktach nie są jeszcze gotowe");
    }

    /** Duplikat SKU → 409 Conflict z krótkim komunikatem w JSON. Pełna obsługa błędów: dział s06_error_handling. */
    @ExceptionHandler(DuplicateSkuException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> duplicate(DuplicateSkuException e) {
        return Map.of("error", e.getMessage());
    }
}
