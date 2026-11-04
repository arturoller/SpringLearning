package s04_web_rest.solutions.rest03_responses;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rest03Responses — zajrzyj dopiero po własnej próbie! */
@RestController
@RequestMapping("/rest03/cw")
public class Rest03SolutionsController {

    /** Rozwiązanie 4 (część 1): status 402 przypisany do wyjątku. */
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public static class PaymentRejectedException extends RuntimeException {
        public PaymentRejectedException() {
            super("Płatność odrzucona");
        }
    }

    /** Rozwiązanie 1: ok(...) albo notFound().build(). */
    @GetMapping("/orders/{id}")
    public ResponseEntity<String> order(@PathVariable long id) {
        return id == 1 ? ResponseEntity.ok("zamówienie 1") : ResponseEntity.notFound().build();
    }

    /** Rozwiązanie 2: noContent() nie ma ciała, więc typ Void. */
    @DeleteMapping("/orders/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable long id) {
        return ResponseEntity.noContent().build();
    }

    /** Rozwiązanie 3: Location zbudowany z bieżącego adresu serwera; created(...) ustawia 201. */
    @PostMapping("/orders")
    public ResponseEntity<String> createOrder(UriComponentsBuilder uriBuilder) {
        URI location = uriBuilder.path("/rest03/cw/orders/{id}").buildAndExpand(42).toUri();
        return ResponseEntity.created(location).body("utworzono");
    }

    /** Rozwiązanie 4 (część 2): rzucenie wyjątku z adnotacją daje 402. */
    @PostMapping("/orders/{id}/pay")
    public String pay(@PathVariable long id) {
        if (id == 13) {
            throw new PaymentRejectedException();
        }
        return "opłacono";
    }

    /** Rozwiązanie 5: nagłówki dopisujemy przed ciałem, na budowniczym odpowiedzi. */
    @GetMapping("/report")
    public ResponseEntity<String> report() {
        return ResponseEntity.ok()
                .header("X-Total-Count", "3")
                .cacheControl(CacheControl.noStore())
                .body("raport");
    }
}
