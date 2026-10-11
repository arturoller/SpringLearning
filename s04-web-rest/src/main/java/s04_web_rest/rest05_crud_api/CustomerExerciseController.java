package s04_web_rest.rest05_crud_api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CustomerExerciseController = API klientów — SZKIELET ćwiczeń lekcji Rest05CrudApi. Magazyn (mapa) i adresy są
 * gotowe; uzupełnij ciała metod. Test: Rest05CrudApiExercisesTest.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerExerciseController {

    /** Customer = klient. */
    public record Customer(long id, String name, String email) {
    }

    /** NewCustomer = dane nowego klienta (ciało POST). */
    public record NewCustomer(String name, String email) {
    }

    /** CustomerPatch = częściowa zmiana klienta (ciało PATCH); null = bez zmian. */
    public record CustomerPatch(String name, String email) {
    }

    /** Klienci: id → klient. Na start: 1 Anna, 2 Bartek. Następne id: 3. */
    final ConcurrentHashMap<Long, Customer> customers = new ConcurrentHashMap<>();
    final AtomicLong nextId = new AtomicLong(3);

    public CustomerExerciseController() {
        customers.put(1L, new Customer(1, "Anna", "anna@example.com"));
        customers.put(2L, new Customer(2, "Bartek", "bartek@example.com"));
    }

    /**
     * ĆWICZENIE 1 (łatwe): {@code GET /api/customers/1} → 200 i klient; nieznane id → 404.
     * Podpowiedź: ResponseEntity.of(Optional.ofNullable(customers.get(id))).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> one(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): {@code POST /api/customers} → zapisz klienta z kolejnym id (nextId) i odpowiedz 201,
     * nagłówkiem Location (pełny adres …/api/customers/{id}) i klientem w ciele. Pusta nazwa (null lub same spacje)
     * → 400 bez zapisu. Podpowiedź: jak create w ProductCrudController (ServletUriComponentsBuilder).
     */
    @PostMapping
    public ResponseEntity<Customer> create(@RequestBody NewCustomer newCustomer) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): {@code PATCH /api/customers/{id}} zmienia tylko przysłane pola (null = bez zmian) →
     * 200 i klient po zmianie; brak klienta → 404. {@code DELETE /api/customers/{id}} → 204, a gdy klienta nie ma → 404.
     * Zmiana musi być niepodzielna (bezpieczna dla wielu wątków). Podpowiedź: customers.computeIfPresent(id, (k, old) ->
     * new Customer(...)) zwraca nową wartość albo null; customers.remove(id) zwraca usuniętą wartość albo null.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Customer> patch(@PathVariable long id, @RequestBody CustomerPatch patch) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Część ćwiczenia 3 — opis wyżej, przy patch(...). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
