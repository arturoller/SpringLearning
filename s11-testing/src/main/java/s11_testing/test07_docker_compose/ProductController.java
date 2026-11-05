package s11_testing.test07_docker_compose;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST lekcji Test07DockerCompose. Punkt końcowy (endpoint) {@code /test07/database} pokazuje, z JAKĄ bazą połączyła
 * się aplikacja — dowód, że adres przyszedł z compose, choć nikt nie wpisał {@code spring.datasource.url}.
 * (Taki punkt końcowy to pomoc dydaktyczna — w prawdziwej aplikacji nie zdradzamy szczegółów bazy.)
 */
@RestController
@RequestMapping("/test07")
public class ProductController {

    private final ProductRepository repository;
    private final JdbcClient jdbc;

    public ProductController(ProductRepository repository, JdbcClient jdbc) {
        this.repository = repository;
        this.jdbc = jdbc;
    }

    public record ProductResponse(long id, String name, BigDecimal price) {
    }

    @GetMapping("/products")
    public List<ProductResponse> list() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(p -> new ProductResponse(p.getId(), p.getName(), p.getPrice()))
                .toList();
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        return repository.findById(id)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(new ProductResponse(p.getId(), p.getName(), p.getPrice())))
                .orElseGet(() -> {
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Brak produktu " + id);
                    problem.setTitle("Nie ma takiego produktu");
                    return ResponseEntity.of(problem).build();
                });
    }

    /** Nazwa bazy, użytkownik i wersja serwera — z samej bazy (SQL PostgreSQL). */
    @GetMapping("/database")
    public Map<String, String> database() {
        return jdbc.sql("SELECT current_database() AS db, current_user AS usr, current_setting('server_version') AS ver")
                .query((rs, n) -> Map.of("database", rs.getString("db"), "user", rs.getString("usr"),
                        "version", rs.getString("ver")))
                .single();
    }
}
