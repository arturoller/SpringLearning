package s04_web_rest.rest05_crud_api;

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
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

/**
 * Kontroler API produktów — cienka warstwa: zamienia HTTP na wywołania serwisu i wynik serwisu na kod statusu.
 * Cała logika (id, sortowanie, podmiana) jest w {@link ProductService}.
 */
@RestController
@RequestMapping("/rest05/products")
public class ProductController {

    private final ProductService service;

    // wstrzykiwanie przez konstruktor (constructor injection) — jedyny konstruktor, więc bez @Autowired
    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return service.findAll().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable long id) {
        return ResponseEntity.of(service.find(id).map(ProductResponse::from));   // 200 albo 404
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody ProductRequest request, UriComponentsBuilder uriBuilder) {
        if (!isValid(request)) {
            return ResponseEntity.badRequest().build();
        }
        ProductResponse created = ProductResponse.from(service.create(request));
        URI location = uriBuilder.path("/rest05/products/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> replace(@PathVariable long id, @RequestBody ProductRequest request) {
        if (!isValid(request)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.of(service.replace(id, request).map(ProductResponse::from));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> changePrice(@PathVariable long id, @RequestBody PricePatch patch) {
        if (patch.price() == null || patch.price().signum() < 0) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.of(service.changePrice(id, patch.price()).map(ProductResponse::from));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** Minimalna kontrola ręczna; deklaratywnie (@Valid, @NotBlank) robi to dział s05_validation. */
    private static boolean isValid(ProductRequest request) {
        BigDecimal price = request.price();
        return request.name() != null && !request.name().isBlank() && price != null && price.signum() >= 0;
    }
}
