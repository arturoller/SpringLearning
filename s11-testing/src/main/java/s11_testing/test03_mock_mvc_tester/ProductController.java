package s11_testing.test03_mock_mvc_tester;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Kontroler produktów — materiał do ćwiczenia asercji MockMvcTester. */
@RestController
@RequestMapping("/test03")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public List<ProductResponse> list() {
        return service.findAll();
    }

    @GetMapping("/products/{id}")
    public ProductResponse get(@PathVariable long id) {
        return service.get(id);   // brak produktu → ProductNotFoundException → advice → 404
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request, UriComponentsBuilder uriBuilder) {
        ProductResponse created = service.create(request);
        URI location = uriBuilder.path("/test03/products/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** Celowa awaria: wyjątek, którego nikt nie obsługuje (nie ma na niego @ExceptionHandler). */
    @GetMapping("/boom")
    public String boom() {
        throw new IllegalStateException("Awaria serwisu");
    }
}
