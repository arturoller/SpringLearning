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
import org.springframework.web.util.UriComponentsBuilder;
import s04_web_rest.rest05_crud_api.Customer;
import s04_web_rest.rest05_crud_api.CustomerBook;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rest05CrudApi — zajrzyj dopiero po własnej próbie! */
@RestController
@RequestMapping("/rest05/cw/customers")
public class Rest05SolutionsController {

    public record CustomerRequest(String name, String email) {
    }

    public record EmailPatch(String email) {
    }

    private final CustomerBook book;

    public Rest05SolutionsController(CustomerBook book) {
        this.book = book;
    }

    /** Rozwiązanie 1: ResponseEntity.of(Optional) → 200 albo 404. */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> get(@PathVariable long id) {
        return ResponseEntity.of(book.find(id));
    }

    /** Rozwiązanie 2: wynik boolean z serwisu zamieniony na 204 albo 404. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return book.remove(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** Rozwiązanie 3: created(location) z adresem zbudowanym z uriBuilder. */
    @PostMapping
    public ResponseEntity<Customer> create(@RequestBody CustomerRequest request, UriComponentsBuilder uriBuilder) {
        Customer created = book.add(request.name(), request.email());
        return ResponseEntity.created(uriBuilder.path("/rest05/cw/customers/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    /** Rozwiązanie 4. */
    @PutMapping("/{id}")
    public ResponseEntity<Customer> rename(@PathVariable long id, @RequestBody CustomerRequest request) {
        return ResponseEntity.of(book.rename(id, request.name()));
    }

    /** Rozwiązanie 5: najpierw sprawdź dane (400), potem zmień (200/404). */
    @PatchMapping("/{id}")
    public ResponseEntity<Customer> changeEmail(@PathVariable long id, @RequestBody EmailPatch patch) {
        if (patch.email() == null || patch.email().isBlank() || !patch.email().contains("@")) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.of(book.changeEmail(id, patch.email()));
    }
}
