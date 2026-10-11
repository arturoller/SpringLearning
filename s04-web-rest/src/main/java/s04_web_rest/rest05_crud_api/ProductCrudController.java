package s04_web_rest.rest05_crud_api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import s04_web_rest.rest05_crud_api.CrudModels.Product;
import s04_web_rest.rest05_crud_api.CrudModels.ProductPatch;
import s04_web_rest.rest05_crud_api.CrudModels.ProductRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * ProductCrudController = pełne API CRUD produktów (create, read, update, delete = utwórz, odczytaj, zmień, usuń).
 * <pre>
 *   GET    /api/products        200 + lista
 *   GET    /api/products/{id}   200 + produkt | 404
 *   POST   /api/products        201 + Location + produkt | 400
 *   PUT    /api/products/{id}   200 + produkt (zastąpiony w całości) | 400 | 404
 *   PATCH  /api/products/{id}   200 + produkt (zmienione wybrane pola) | 400 | 404
 *   DELETE /api/products/{id}   204 | 404
 * </pre>
 * Kontroler jest „cienki”: tłumaczy HTTP na wywołania repozytorium i wyniki na kody statusu.
 */
@RestController
@RequestMapping("/api/products")
public class ProductCrudController {

    private final ProductRepository repository;

    public ProductCrudController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Product> all() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> one(@PathVariable long id) {
        return ResponseEntity.of(repository.findById(id));
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody ProductRequest request) {
        validate(request.name(), request.priceGrosze());
        Product created = repository.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> replace(@PathVariable long id, @RequestBody ProductRequest request) {
        validate(request.name(), request.priceGrosze());
        return ResponseEntity.of(repository.replace(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Product> patch(@PathVariable long id, @RequestBody ProductPatch patch) {
        // sprawdzamy tylko pola, które klient przysłał (null = „bez zmian”)
        if (patch.name() != null && patch.name().isBlank()) {
            throw new IllegalArgumentException("Nazwa produktu nie może być pusta");
        }
        if (patch.priceGrosze() != null && patch.priceGrosze() < 0) {
            throw new IllegalArgumentException("Cena nie może być ujemna: " + patch.priceGrosze());
        }
        return ResponseEntity.of(repository.patch(id, patch));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return repository.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** Prosta ręczna walidacja — porządnie (adnotacje @NotBlank, @Min) w dziale s05_validation. */
    private static void validate(String name, int priceGrosze) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nazwa produktu nie może być pusta");
        }
        if (priceGrosze < 0) {
            throw new IllegalArgumentException("Cena nie może być ujemna: " + priceGrosze);
        }
    }

    /** IllegalArgumentException z walidacji → 400 z komunikatem (obsługa błędów — dział s06_error_handling). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
