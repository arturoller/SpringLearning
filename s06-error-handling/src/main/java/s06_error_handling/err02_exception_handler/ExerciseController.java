package s06_error_handling.err02_exception_handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * SZKIELET ĆWICZEŃ lekcji Err02ExceptionHandler — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Err02ExceptionHandlerExercisesTest). Mapowania punktów końcowych są gotowe. Dopóki metody rzucają
 * UnsupportedOperationException("TODO"), testy ćwiczeń nie przechodzą (są wyłączone z domyślnego {@code ./mvnw test}).
 * Metody handlerów dopisujesz sam (pod komentarzami), z odpowiednimi adnotacjami.
 */
@RestController
@RequestMapping("/err02/cw")
public class ExerciseController {

    public static class CustomerNotFoundException extends RuntimeException {

        public CustomerNotFoundException(long id) {
            super("Nie ma klienta " + id);
        }
    }

    public static class OrderAlreadyShippedException extends RuntimeException {

        public OrderAlreadyShippedException(long id) {
            super("Zamówienie " + id + " zostało już wysłane");
        }
    }

    public static class InvoiceException extends RuntimeException {

        public InvoiceException(String message) {
            super(message);
        }
    }

    public static class InvoiceExpiredException extends InvoiceException {

        public InvoiceExpiredException() {
            super("Faktura jest przeterminowana");
        }
    }

    public static class WarehouseClosedException extends RuntimeException {
    }

    public static class WarehouseOverloadedException extends RuntimeException {
    }

    /** Dane nowego klienta (ograniczenia już dodane). */
    public record CustomerRequest(@NotBlank(message = "Imię jest wymagane") String name,
                                  @Email(message = "Zły adres e-mail") @NotBlank(message = "E-mail jest wymagany") String email) {
    }

    // ------------------------------------------------------------------ punkty końcowe (ciała do uzupełnienia)

    /** Ćwiczenie 1: dla id 1 zwróć {@code "Anna"}, dla innych rzuć {@link CustomerNotFoundException}. */
    @GetMapping("/customers/{id}")
    public String customer(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 2: zawsze rzuć {@link OrderAlreadyShippedException} dla podanego id. */
    @PostMapping("/orders/{id}/ship")
    public String ship(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 3: dla {@code "stare"} rzuć {@link InvoiceExpiredException}, dla {@code "bledna"} — {@link InvoiceException}
     * z opisem {@code "Faktura jest błędna"}, dla innych zwróć {@code "OK"}.
     */
    @GetMapping("/invoices/{kind}")
    public String invoice(@PathVariable String kind) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 4: utwórz klienta (zwróć {@code "Utworzono: <name>"}); błędne dane obsłuż handlerem walidacji. */
    @PostMapping("/customers")
    public String create(@RequestBody CustomerRequest request) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 5: dla {@code "closed"} rzuć {@link WarehouseClosedException}, dla {@code "overloaded"} —
     * {@link WarehouseOverloadedException}, dla innych zwróć {@code "OK"}.
     */
    @GetMapping("/warehouse/{mode}")
    public String warehouse(@PathVariable String mode) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // ------------------------------------------------------------------ handlery: dopisz je tutaj (ćwiczenia 1-5)
    //
    // Ćwiczenie 1: handler dla CustomerNotFoundException → ResponseEntity<ApiError> 404, kod "CUSTOMER_NOT_FOUND",
    //              message z wyjątku, path z HttpServletRequest.
    // Ćwiczenie 2: handler dla OrderAlreadyShippedException → rekord ApiError ze statusem 409 z @ResponseStatus,
    //              kod "ALREADY_SHIPPED".
    // Ćwiczenie 3: dwa handlery: InvoiceException → 400, kod "INVOICE_ERROR"; InvoiceExpiredException → 410 (Gone), kod "INVOICE_EXPIRED".
    // Ćwiczenie 4: handler dla MethodArgumentNotValidException → 400 i posortowana mapa pole → komunikat (TreeMap).
    // Ćwiczenie 5: jeden handler dla OBU wyjątków magazynu (jedna adnotacja z dwoma typami) → 503, kod = prosta nazwa
    //              klasy wyjątku (exception.getClass().getSimpleName()), message "Magazyn niedostępny".
}
