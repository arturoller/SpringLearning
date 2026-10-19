package s04_web_rest.rest05_crud_api;

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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ćwiczenia lekcji Rest05CrudApi — CRUD klientów pod /api/cw05/customers. Testy: Rest05CrudApiExercisesTest
 * (uruchamiasz sam: ▶ obok klasy testu). Model, DTO, mapa i licznik są gotowe — uzupełniasz metody kontrolera.
 */
@RestController
@RequestMapping("/api/cw05/customers")
public class Rest05ExerciseController {

    /** Model klienta (w środku sklepu) — email jest daną osobową, NIE wysyłamy go w odpowiedziach. */
    public record Customer(long id, String name, String city, String email, boolean vip) {
    }

    /** POST — nowy klient (id nadaje serwer). */
    public record CreateCustomerRequest(String name, String city, String email, boolean vip) {
    }

    /** PUT — wszystkie pola (bez id — id jest w adresie). */
    public record ReplaceCustomerRequest(String name, String city, String email, boolean vip) {
    }

    /** PATCH — tylko miasto i/lub status VIP; null = bez zmian. */
    public record PatchCustomerRequest(String city, Boolean vip) {
    }

    /** Odpowiedź — BEZ emaila. */
    public record CustomerResponse(long id, String name, String city, boolean vip) {
    }

    final Map<Long, Customer> customers = new ConcurrentHashMap<>();
    final AtomicLong nextId = new AtomicLong(8);

    public Rest05ExerciseController() {
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

    /**
     * ĆWICZENIE 1 (łatwe): GET /api/cw05/customers → lista CustomerResponse posortowana po id (mapa nie ma kolejności!)
     * oraz GET /api/cw05/customers/{id} → 200 i CustomerResponse albo 404.
     * Podpowiedź: pomocnicza metoda toResponse(Customer) przyda się we wszystkich ćwiczeniach.
     */
    @GetMapping
    public List<CustomerResponse> all() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Część ĆWICZENIA 1. */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> one(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): POST /api/cw05/customers → 201 Created, Location z adresem nowego klienta, CustomerResponse
     * w ciele (bez emaila!). id z nextId.getAndIncrement().
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@RequestBody CreateCustomerRequest request) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PUT /api/cw05/customers/{id} → zastąp CAŁEGO klienta (200 + CustomerResponse) albo 404,
     * gdy go nie ma (PUT nie tworzy klientów). Dwa takie same PUT-y mają dać ten sam stan (idempotentność).
     * Podpowiedź: {@code customers.replace(id, nowy) != null} — atomowo „zastąp, jeśli jest”.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> replace(@PathVariable long id, @RequestBody ReplaceCustomerRequest request) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): PATCH /api/cw05/customers/{id} — zmień TYLKO pola różne od null (city, vip), resztę
     * (imię, email!) zostaw. 200 + CustomerResponse albo 404. Pusty PATCH {} nic nie zmienia.
     * Podpowiedź: {@code customers.computeIfPresent(id, (key, current) -> ...)} — atomowo i zwraca nową wartość albo null.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<CustomerResponse> patch(@PathVariable long id, @RequestBody PatchCustomerRequest request) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): DELETE /api/cw05/customers/{id} → 204, gdy klient był; 404, gdy go nie ma. Potem
     * przejdź w teście exercise5 cały cykl: POST → GET → PATCH → PUT → DELETE → DELETE → GET.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
