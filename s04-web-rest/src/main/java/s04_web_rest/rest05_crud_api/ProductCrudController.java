package s04_web_rest.rest05_crud_api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import s04_web_rest.rest05_crud_api.ProductDtos.CreateProductRequest;
import s04_web_rest.rest05_crud_api.ProductDtos.PatchProductRequest;
import s04_web_rest.rest05_crud_api.ProductDtos.ProductResponse;
import s04_web_rest.rest05_crud_api.ProductDtos.ReplaceProductRequest;

import java.net.URI;
import java.util.List;

/**
 * ProductCrudController = pełne CRUD produktów (Create, Read, Update, Delete = utwórz, odczytaj, zmień, usuń).
 * Przyjmuje i zwraca WYŁĄCZNIE DTO (ProductDtos), a model Product zna tylko z repozytorium.
 *
 * <pre>
 *   GET    /api/products          200 lista
 *   GET    /api/products/{sku}    200 / 404
 *   POST   /api/products          201 + Location / 409 (sku zajęty)
 *   PUT    /api/products/{sku}    200 / 404     — zastąpienie całości, idempotentne
 *   PATCH  /api/products/{sku}    200 / 404     — zmiana części (null = bez zmian)
 *   DELETE /api/products/{sku}    204 / 404     — idempotentne (stan po 1. i 2. wywołaniu ten sam)
 * </pre>
 */
@RestController
@RequestMapping("/api/products")
public class ProductCrudController {

    private final ProductRepository repository;

    public ProductCrudController(ProductRepository repository) {   // wstrzykiwanie przez konstruktor
        this.repository = repository;
    }

    @GetMapping
    public List<ProductResponse> all() {
        return repository.findAll().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ProductResponse> one(@PathVariable String sku) {
        return ResponseEntity.of(repository.findBySku(sku).map(ProductResponse::from));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody CreateProductRequest request) {
        Product product = request.toProduct();
        if (!repository.saveIfAbsent(product)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Produkt " + product.sku() + " już istnieje");
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{sku}")
                .buildAndExpand(product.sku())
                .toUri();
        return ResponseEntity.created(location).body(ProductResponse.from(product));
    }

    @PutMapping("/{sku}")
    public ResponseEntity<ProductResponse> replace(@PathVariable String sku,
                                                   @RequestBody ReplaceProductRequest request) {
        Product product = request.toProduct(sku);   // sku z ADRESU — nie z ciała
        return repository.replaceIfPresent(product)
                ? ResponseEntity.ok(ProductResponse.from(product))
                : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{sku}")
    public ResponseEntity<ProductResponse> patch(@PathVariable String sku, @RequestBody PatchProductRequest request) {
        return repository.findBySku(sku)
                .map(current -> applyPatch(current, request))
                .map(repository::save)
                .map(ProductResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> delete(@PathVariable String sku) {
        return repository.deleteBySku(sku)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /** Zmienia tylko pola, które klient podał (różne od null). */
    private static Product applyPatch(Product current, PatchProductRequest patch) {
        Product result = current;
        if (patch.name() != null) {
            result = result.withName(patch.name());
        }
        if (patch.price() != null) {
            result = result.withPrice(patch.price());
        }
        if (patch.stock() != null) {
            result = result.withStock(patch.stock());
        }
        return result;
    }
}
