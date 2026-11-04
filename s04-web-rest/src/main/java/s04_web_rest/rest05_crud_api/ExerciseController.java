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
import org.springframework.web.util.UriComponentsBuilder;

/**
 * SZKIELET ĆWICZEŃ lekcji Rest05CrudApi — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Rest05CrudApiExercisesTest). Budujesz CRUD dla klientów na gotowym serwisie {@link CustomerBook}. Mapowania są gotowe.
 * Dopóki metody rzucają UnsupportedOperationException("TODO"), testy ćwiczeń nie przechodzą
 * (są wyłączone z domyślnego {@code ./mvnw test}).
 */
@RestController
@RequestMapping("/rest05/cw/customers")
public class ExerciseController {

    /** Dane wejściowe dla POST i PUT. */
    public record CustomerRequest(String name, String email) {
    }

    /** Dane wejściowe dla PATCH: zmiana samego e-maila. */
    public record EmailPatch(String email) {
    }

    private final CustomerBook book;

    public ExerciseController(CustomerBook book) {
        this.book = book;
    }

    /** Ćwiczenie 1: 200 z klientem albo 404 bez ciała, gdy go nie ma. */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> get(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 2: 204 bez ciała, gdy klient został usunięty; 404, gdy go nie było. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 3: dodaj klienta; odpowiedź 201, nagłówek Location (/rest05/cw/customers/{id}) i klient w ciele. */
    @PostMapping
    public ResponseEntity<Customer> create(@RequestBody CustomerRequest request, UriComponentsBuilder uriBuilder) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 4: zmień nazwę klienta (book.rename); 200 z klientem albo 404. */
    @PutMapping("/{id}")
    public ResponseEntity<Customer> rename(@PathVariable long id, @RequestBody CustomerRequest request) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 5: zmień sam e-mail (book.changeEmail). Gdy e-mail jest pusty (null/biały) albo nie zawiera znaku '@' —
     * 400 (i nic nie zmieniaj). Inaczej 200 z klientem albo 404, gdy klienta nie ma.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Customer> changeEmail(@PathVariable long id, @RequestBody EmailPatch patch) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
