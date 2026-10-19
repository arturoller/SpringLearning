package s06_error_handling.err02_exception_handler;

/** Ogólny błąd płatności — korzeń małej hierarchii wyjątków (PaymentException ← CardDeclinedException). */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }
}
