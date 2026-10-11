package s06_error_handling.err03_controller_advice;

/** Płatność odrzucona przez bank. Obsługują ją dwa advice o różnej kolejności (@Order) — wygrywa pierwszy. */
public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(String message) {
        super(message);
    }
}
