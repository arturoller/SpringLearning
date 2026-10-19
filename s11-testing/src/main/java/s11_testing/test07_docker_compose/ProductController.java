package s11_testing.test07_docker_compose;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Kontroler do sprawdzania ręcznego (s11-testing/http/test07_docker_compose.http): produkty i nazwa bazy — po
 * uruchomieniu Test07Application widać „PostgreSQL 18…” z kontenera, który uruchomił sam Spring Boot.
 */
@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/products")
    public List<Product> products() {
        return repository.findAll();
    }

    @GetMapping("/database")
    public Map<String, String> database() {
        return Map.of("database", repository.databaseVersion());
    }
}
