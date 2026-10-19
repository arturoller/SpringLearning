package s11_testing.test06_testcontainers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Mały kontroler do sprawdzania ręcznego (plik s11-testing/http/test06_testcontainers.http): lista klientów i nazwa
 * bazy, z którą aplikacja naprawdę rozmawia — po uruchomieniu z kontenerem widać „PostgreSQL 18…”.
 */
@RestController
@RequestMapping("/api")
public class CustomerController {

    private final CustomerRepository repository;

    public CustomerController(CustomerRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/customers")
    public List<Customer> customers() {
        return repository.findAll();
    }

    @GetMapping("/database")
    public Map<String, String> database() {
        return Map.of("database", repository.databaseVersion());
    }
}
