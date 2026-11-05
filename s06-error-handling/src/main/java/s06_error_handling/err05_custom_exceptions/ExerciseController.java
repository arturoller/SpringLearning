package s06_error_handling.err05_custom_exceptions;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SZKIELET ĆWICZEŃ lekcji Err05CustomExceptions — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Err05CustomExceptionsExercisesTest). Mapowania i hierarchia wyjątków są gotowe. Dopóki metody rzucają
 * UnsupportedOperationException("TODO"), testy ćwiczeń nie przechodzą (są wyłączone z domyślnego {@code ./mvnw test}).
 * Handlery piszesz w klasie {@link ExerciseAdvice}.
 */
@RestController
@RequestMapping("/err05/cw")
public class ExerciseController {

    /** Korzeń małej hierarchii ćwiczeń (odpowiednik ShopException). */
    public abstract static class ExerciseException extends RuntimeException {

        private final String code;

        protected ExerciseException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public abstract static class ExerciseNotFoundException extends ExerciseException {

        protected ExerciseNotFoundException(String code, String message) {
            super(code, message);
        }
    }

    public static class CustomerNotFoundException extends ExerciseNotFoundException {

        public CustomerNotFoundException(long id) {
            super("CUSTOMER_NOT_FOUND", "Nie ma klienta " + id);
        }
    }

    public abstract static class ExerciseRuleException extends ExerciseException {

        protected ExerciseRuleException(String code, String message) {
            super(code, message);
        }
    }

    public static class PaymentDeclinedException extends ExerciseRuleException {

        public PaymentDeclinedException(long id) {
            super("PAYMENT_DECLINED", "Płatność " + id + " została odrzucona");
        }
    }

    public static class InvoiceAlreadyPaidException extends ExerciseRuleException {

        public InvoiceAlreadyPaidException(long id) {
            super("INVOICE_ALREADY_PAID", "Faktura " + id + " jest już opłacona");
        }
    }

    /** Wyjątek poza hierarchią ćwiczeń: magazyn jest zajęty, spróbuj za {@code retryAfterSeconds} sekund. */
    public static class WarehouseBusyException extends RuntimeException {

        private final int retryAfterSeconds;

        public WarehouseBusyException(int retryAfterSeconds) {
            super("Magazyn jest zajęty");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public int getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    /** Ćwiczenie 1: dla id 1 zwróć {@code "Anna"}, dla innych rzuć {@link CustomerNotFoundException}. */
    @GetMapping("/customers/{id}")
    public String customer(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 2: zawsze rzuć {@link PaymentDeclinedException} dla podanego id. */
    @PostMapping("/payments/{id}")
    public String pay(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 3: zawsze rzuć {@link InvoiceAlreadyPaidException} dla podanego id. */
    @PostMapping("/invoices/{id}/pay")
    public String payInvoice(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 4: rzuć {@code new IllegalStateException("tajny szczegół: hasło do bazy to 'admin'")}. */
    @GetMapping("/crash")
    public String crash() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 5: zawsze rzuć {@code new WarehouseBusyException(30)}. */
    @PostMapping("/stock/refresh")
    public String refreshStock() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
