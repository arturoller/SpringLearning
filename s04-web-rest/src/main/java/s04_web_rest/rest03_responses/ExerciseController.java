package s04_web_rest.rest03_responses;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * SZKIELET ĆWICZEŃ lekcji Rest03Responses — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Rest03ResponsesExercisesTest). Mapowania są gotowe, Ty ustawiasz statusy, nagłówki i ciała odpowiedzi.
 * Dopóki metody rzucają UnsupportedOperationException("TODO"), testy ćwiczeń nie przechodzą
 * (są wyłączone z domyślnego {@code ./mvnw test}).
 */
@RestController
@RequestMapping("/rest03/cw")
public class ExerciseController {

    /** Ćwiczenie 4: wyjątek bez statusu — nadaj mu odpowiedni kod adnotacją (nad klasą). */
    public static class PaymentRejectedException extends RuntimeException {
        public PaymentRejectedException() {
            super("Płatność odrzucona");
        }
    }

    /** Ćwiczenie 1: dla id = 1 zwróć 200 i ciało "zamówienie 1"; dla każdego innego id — 404 bez ciała. */
    @GetMapping("/orders/{id}")
    public ResponseEntity<String> order(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 2: usuń zamówienie — odpowiedź 204 No Content (bez ciała), niezależnie od id. */
    @DeleteMapping("/orders/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 3: "utwórz" zamówienie o stałym id 42 — odpowiedź 201 Created, nagłówek Location z adresem
     * {@code /rest03/cw/orders/42} (pełny URL zbuduj z uriBuilder) i ciało "utworzono".
     */
    @PostMapping("/orders")
    public ResponseEntity<String> createOrder(UriComponentsBuilder uriBuilder) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 4: płatność za zamówienie 13 jest odrzucana — rzuć PaymentRejectedException, klient ma dostać 402. */
    @PostMapping("/orders/{id}/pay")
    public String pay(@PathVariable long id) {
        // TODO: twoje rozwiązanie (dla id różnego od 13 zwróć "opłacono")
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 5: 200 z ciałem "raport", nagłówkiem X-Total-Count: 3 i nagłówkiem Cache-Control: no-store. */
    @GetMapping("/report")
    public ResponseEntity<String> report() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
