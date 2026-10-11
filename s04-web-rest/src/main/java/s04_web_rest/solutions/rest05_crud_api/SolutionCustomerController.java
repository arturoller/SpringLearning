package s04_web_rest.solutions.rest05_crud_api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import s04_web_rest.rest05_crud_api.CustomerExerciseController.Customer;
import s04_web_rest.rest05_crud_api.CustomerExerciseController.CustomerPatch;
import s04_web_rest.rest05_crud_api.CustomerExerciseController.NewCustomer;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Rozwiązania wzorcowe ćwiczeń 1–3 lekcji Rest05CrudApi — zajrzyj dopiero po własnej próbie! */
@RestController
@RequestMapping("/api/customers")
public class SolutionCustomerController {

    final ConcurrentHashMap<Long, Customer> customers = new ConcurrentHashMap<>();
    final AtomicLong nextId = new AtomicLong(3);

    public SolutionCustomerController() {
        customers.put(1L, new Customer(1, "Anna", "anna@example.com"));
        customers.put(2L, new Customer(2, "Bartek", "bartek@example.com"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> one(@PathVariable long id) {
        return ResponseEntity.of(Optional.ofNullable(customers.get(id)));
    }

    @PostMapping
    public ResponseEntity<Customer> create(@RequestBody NewCustomer newCustomer) {
        if (newCustomer.name() == null || newCustomer.name().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        long id = nextId.getAndIncrement();
        Customer customer = new Customer(id, newCustomer.name(), newCustomer.email());
        customers.put(id, customer);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(customer);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Customer> patch(@PathVariable long id, @RequestBody CustomerPatch patch) {
        Customer updated = customers.computeIfPresent(id, (key, old) -> new Customer(
                id,
                patch.name() != null ? patch.name() : old.name(),
                patch.email() != null ? patch.email() : old.email()));
        return ResponseEntity.of(Optional.ofNullable(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return customers.remove(id) != null ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
