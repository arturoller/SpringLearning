package s04_web_rest.solutions.rest03_responses;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import s04_web_rest.rest03_responses.Rest03ExerciseController.Customer;
import s04_web_rest.rest03_responses.Rest03ExerciseController.NewCustomer;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Rest03Responses — zajrzyj dopiero po własnej próbie!
 * Rekordy Customer i NewCustomer pochodzą ze szkieletu ćwiczeń.
 */
@RestController
@RequestMapping("/api/cw03/customers")
public class Rest03SolutionController {

    private final Map<Long, Customer> customers = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(8);

    public Rest03SolutionController() {
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

    /** Rozwiązanie 1: Optional z mapy → ResponseEntity.of: jest → 200 + ciało, nie ma → 404. */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> one(@PathVariable long id) {
        return ResponseEntity.of(Optional.ofNullable(customers.get(id)));
    }

    /** Rozwiązanie 2: id z licznika, adres z bieżącego zapytania + /{id}, 201 Created + Location + ciało. */
    @PostMapping
    public ResponseEntity<Customer> create(@RequestBody NewCustomer newCustomer) {
        Customer customer = new Customer(nextId.getAndIncrement(), newCustomer.name(), newCustomer.city(),
                newCustomer.email(), newCustomer.vip());
        add(customer);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(customer.id())
                .toUri();
        return ResponseEntity.created(location).body(customer);
    }

    /** Rozwiązanie 3: remove zwraca usuniętego klienta albo null — od tego zależy 204 albo 404. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return customers.remove(id) != null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /** Rozwiązanie 4: ten sam adres co one(), ale produces = text/plain — Spring wybiera po nagłówku Accept. */
    @GetMapping(value = "/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> card(@PathVariable long id) {
        return Optional.ofNullable(customers.get(id))
                .map(customer -> customer.name() + ", " + customer.city() + (customer.vip() ? " (VIP)" : ""))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
