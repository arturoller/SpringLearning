package s06_error_handling.err02_exception_handler;

/** Wyjątek domenowy: brak towaru w magazynie. */
public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String productName) {
        super("Brak towaru: " + productName);
    }
}
