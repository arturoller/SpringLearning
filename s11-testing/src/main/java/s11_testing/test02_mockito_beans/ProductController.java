package s11_testing.test02_mockito_beans;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/** Kontroler produktów — cienka warstwa HTTP nad {@link ProductService}. */
@RestController
@RequestMapping("/test02/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable long id) {
        return service.find(id);
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody ProductRequest request, UriComponentsBuilder uriBuilder) {
        Product created = service.create(request);
        URI location = uriBuilder.path("/test02/products/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** Odświeża cenę z zewnętrznej usługi: 200 z produktem, 404 (brak produktu) albo 503 (usługa nie odpowiada). */
    @PostMapping("/{id}/refresh-price")
    public Product refreshPrice(@PathVariable long id) {
        return service.refreshPrice(id);
    }
}
