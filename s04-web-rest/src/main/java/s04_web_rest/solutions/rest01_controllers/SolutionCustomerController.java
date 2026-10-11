package s04_web_rest.solutions.rest01_controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Rozwiązanie wzorcowe ćwiczeń 1–2 lekcji Rest01Controllers — kontroler klientów. */
@RestController
@RequestMapping("/api/customers")
public class SolutionCustomerController {

    static final List<String> CUSTOMERS = List.of("Anna", "Bartek", "Celina");

    @GetMapping
    public List<String> all() {
        return CUSTOMERS;
    }

    /** Pełny adres = "/api/customers" z klasy + "/count" z metody. Tylko GET, więc DELETE dostanie 405. */
    @GetMapping("/count")
    public int count() {
        return CUSTOMERS.size();
    }
}
