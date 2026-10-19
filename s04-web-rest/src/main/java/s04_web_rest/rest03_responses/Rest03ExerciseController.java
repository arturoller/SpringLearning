package s04_web_rest.rest03_responses;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ćwiczenia lekcji Rest03Responses — klienci sklepu pod /api/cw03/customers. Testy: Rest03ResponsesExercisesTest
 * (uruchamiasz sam: ▶ obok klasy testu). Mapa i licznik są już gotowe — uzupełniasz odpowiedzi HTTP.
 */
@RestController
@RequestMapping("/api/cw03/customers")
public class Rest03ExerciseController {

    /** Klient sklepu. email może być null (Maria Nowak i Ola Pawlak nie podały adresu). */
    public record Customer(long id, String name, String city, String email, boolean vip) {
    }

    /** Dane nowego klienta w ciele POST — bez id, id nadaje serwer. */
    public record NewCustomer(String name, String city, String email, boolean vip) {
    }

    final Map<Long, Customer> customers = new ConcurrentHashMap<>();
    final AtomicLong nextId = new AtomicLong(8);   // klienci 1–7 już są; AtomicLong — licznik bezpieczny dla wątków

    public Rest03ExerciseController() {
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
     * ĆWICZENIE 1 (łatwe): GET /api/cw03/customers/1 → 200 i klient w JSON; GET /api/cw03/customers/99 → 404 z pustym
     * ciałem. Podpowiedź: {@code ResponseEntity.of(Optional.ofNullable(...))}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> one(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): POST /api/cw03/customers z NewCustomer w ciele → nowy klient z id z licznika nextId,
     * status 201, nagłówek Location z pełnym adresem (w MockMvc: http://localhost/api/cw03/customers/{id}) i klient
     * w ciele. Podpowiedź: ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")...
     */
    @PostMapping
    public ResponseEntity<Customer> create(@RequestBody NewCustomer newCustomer) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): DELETE /api/cw03/customers/{id} → 204 No Content, gdy klient był; 404, gdy go nie ma.
     * Podpowiedź: {@code customers.remove(id)} zwraca usuniętą wartość albo null.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): negocjacja treści. Ten sam adres GET /api/cw03/customers/{id}, ale z nagłówkiem
     * Accept: text/plain ma zwrócić wizytówkę tekstem: "Jan Kowalski, Warszawa (VIP)" (bez „ (VIP)” dla zwykłych
     * klientów); nieznany id → 404. Z Accept: application/json nadal działa JSON z ćwiczenia 1, a Accept:
     * application/xml → 406. Ta metoda nie ma jeszcze MAPOWANIA — dodaj {@code @GetMapping} z adresem i
     * {@code produces = MediaType.TEXT_PLAIN_VALUE}, potem ciało.
     */
    public ResponseEntity<String> card(@PathVariable long id) {
        // TODO: twoje rozwiązanie (dodaj mapowanie z produces)
        throw new UnsupportedOperationException("TODO");
    }
}
