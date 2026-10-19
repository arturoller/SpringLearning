package s04_web_rest.solutions.rest05_crud_api;

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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import s04_web_rest.rest05_crud_api.Rest05ExerciseController.CreateCustomerRequest;
import s04_web_rest.rest05_crud_api.Rest05ExerciseController.Customer;
import s04_web_rest.rest05_crud_api.Rest05ExerciseController.CustomerResponse;
import s04_web_rest.rest05_crud_api.Rest05ExerciseController.PatchCustomerRequest;
import s04_web_rest.rest05_crud_api.Rest05ExerciseController.ReplaceCustomerRequest;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Rest05CrudApi — zajrzyj dopiero po własnej próbie!
 * Model i DTO pochodzą ze szkieletu ćwiczeń (Rest05ExerciseController).
 */
@RestController
@RequestMapping("/api/cw05/customers")
public class Rest05SolutionController {

    private final Map<Long, Customer> customers = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(8);

    public Rest05SolutionController() {
        add(new Customer(1, "Jan Kowalski", "Warszawa", "jan@example.com", true));
        add(new Customer(2, "Maria Nowak", "Kraków", null, false));
        add(new Customer(3, "Adam Mazur", "Gdańsk", "adam.mazur@example.com", false));
        add(new Customer(4, "Zofia Krawczyk", "Warszawa", "zofia@example.com", true));
        add(new Customer(5, "Ola Pawlak", "Poznań", null, false));
        add(new Customer(6, "Marek Król", "Kraków", "marek@example.com", false));
        add(new Customer(7, "Ewa Lis", "Wrocław", "ewa.lis@example.com", false));
    }

    private void add(Customer customer) {
        customers.put(customer.id(), customer);
    }

    /** Jedno miejsce konwersji model → DTO (bez emaila). */
    private static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.id(), customer.name(), customer.city(), customer.vip());
    }

    /** Rozwiązanie 1a: sortowanie przy odczycie — ConcurrentHashMap nie gwarantuje kolejności. */
    @GetMapping
    public List<CustomerResponse> all() {
        return customers.values().stream()
                .sorted(Comparator.comparingLong(Customer::id))
                .map(Rest05SolutionController::toResponse)
                .toList();
    }

    /** Rozwiązanie 1b: Optional → ResponseEntity.of (200 albo 404). */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> one(@PathVariable long id) {
        return ResponseEntity.of(Optional.ofNullable(customers.get(id)).map(Rest05SolutionController::toResponse));
    }

    /** Rozwiązanie 2: id z licznika, 201 + Location, odpowiedź jako DTO. */
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@RequestBody CreateCustomerRequest request) {
        Customer customer = new Customer(nextId.getAndIncrement(), request.name(), request.city(), request.email(),
                request.vip());
        add(customer);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(customer.id())
                .toUri();
        return ResponseEntity.created(location).body(toResponse(customer));
    }

    /** Rozwiązanie 3: replace podmienia wartość tylko dla istniejącego klucza — atomowo. */
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> replace(@PathVariable long id, @RequestBody ReplaceCustomerRequest request) {
        Customer customer = new Customer(id, request.name(), request.city(), request.email(), request.vip());
        return customers.replace(id, customer) != null
                ? ResponseEntity.ok(toResponse(customer))
                : ResponseEntity.notFound().build();
    }

    /** Rozwiązanie 4: computeIfPresent — odczyt, zmiana i zapis w jednej atomowej operacji mapy. */
    @PatchMapping("/{id}")
    public ResponseEntity<CustomerResponse> patch(@PathVariable long id, @RequestBody PatchCustomerRequest request) {
        Customer updated = customers.computeIfPresent(id, (key, current) -> new Customer(
                current.id(),
                current.name(),
                request.city() != null ? request.city() : current.city(),
                current.email(),
                request.vip() != null ? request.vip() : current.vip()));
        return updated != null ? ResponseEntity.ok(toResponse(updated)) : ResponseEntity.notFound().build();
    }

    /** Rozwiązanie 5: remove zwraca usuniętą wartość albo null → 204 albo 404. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return customers.remove(id) != null ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
