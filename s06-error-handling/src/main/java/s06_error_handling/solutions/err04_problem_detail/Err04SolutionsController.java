package s06_error_handling.solutions.err04_problem_detail;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import s06_error_handling.err04_problem_detail.NewCustomer;

import java.net.URI;

/** Rozwiązanie wzorcowe kontrolera ćwiczeń lekcji Err04ProblemDetail. */
@RestController
@RequestMapping("/err04/cw")
public class Err04SolutionsController {

    public static class CustomerNotFoundException extends RuntimeException {

        public CustomerNotFoundException(long id) {
            super("Nie ma klienta " + id);
        }
    }

    @GetMapping("/customers/{id}")
    public String customer(@PathVariable long id) {
        if (id == 1) {
            return "Anna";
        }
        throw new CustomerNotFoundException(id);
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable long id) {
        ErrorResponseException exception = new ErrorResponseException(HttpStatus.CONFLICT);
        exception.setType(URI.create("https://sklep.example/errors/order-locked"));
        exception.setTitle("Zamówienie zablokowane");
        exception.setDetail("Zamówienie " + id + " jest zablokowane");
        exception.getBody().setProperty("code", "ORDER_LOCKED");
        throw exception;
    }

    @GetMapping("/search")
    public String search(@RequestParam String q) {
        return "Szukam: " + q;
    }

    @GetMapping("/crash")
    public String crash() {
        throw new IllegalStateException("tajny szczegół: hasło do bazy to 'admin'");
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public String create(@Valid @RequestBody NewCustomer request) {
        return "Utworzono: " + request.name();
    }
}
