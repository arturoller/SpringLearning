package s06_error_handling.solutions.err05_custom_exceptions;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rozwiązanie wzorcowe kontrolera ćwiczeń lekcji Err05CustomExceptions. */
@RestController
@RequestMapping("/err05/cw")
public class Err05SolutionsController {

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

        @GetMapping("/customers/{id}")
    public String customer(@PathVariable long id) {
        if (id == 1) {
            return "Anna";
        }
        throw new CustomerNotFoundException(id);
    }

        @PostMapping("/payments/{id}")
    public String pay(@PathVariable long id) {
        throw new PaymentDeclinedException(id);
    }

        @PostMapping("/invoices/{id}/pay")
    public String payInvoice(@PathVariable long id) {
        throw new InvoiceAlreadyPaidException(id);
    }

        @GetMapping("/crash")
    public String crash() {
        throw new IllegalStateException("tajny szczegół: hasło do bazy to 'admin'");
    }

        @PostMapping("/stock/refresh")
    public String refreshStock() {
        throw new WarehouseBusyException(30);
    }
}
