package s11_testing.test06_testcontainers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST produktów lekcji Test06Testcontainers. POST używa zapytania {@code ON CONFLICT} — działa tylko na PostgreSQL
 * (na H2 kończy się błędem 500). Test06SpringBootPostgresTest sprawdza cały przepływ na bazie z kontenera.
 */
@RestController
@RequestMapping("/test06/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /** Odpowiedź: nazwa i cena (bez encji w JSON). */
    public record ProductResponse(long id, String name, BigDecimal price) {
        static ProductResponse from(Product product) {
            return new ProductResponse(product.getId(), product.getName(), product.getPrice());
        }
    }

    @GetMapping
    public List<ProductResponse> list() {
        return service.findAll().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/top")
    public List<ProductResponse> top(@RequestParam(defaultValue = "3") int n) {
        return service.mostExpensive(n).stream().map(ProductResponse::from).toList();
    }

    /** 201 Created, gdy produkt dodano; 409 Conflict (ProblemDetail), gdy produkt o tej nazwie już istnieje. */
    @PostMapping
    public ResponseEntity<?> add(@Valid @RequestBody ProductRequest request) {
        if (service.addIfAbsent(request.name(), request.price())) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Produkt '" + request.name() + "' już istnieje");
        problem.setTitle("Produkt już istnieje");
        return ResponseEntity.of(problem).build();
    }
}
