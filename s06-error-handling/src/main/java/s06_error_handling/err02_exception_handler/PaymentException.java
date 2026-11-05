package s06_error_handling.err02_exception_handler;

/** Ogólny problem z płatnością (wyjątek bazowy — sekcja 3: hierarchia). */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }
}
