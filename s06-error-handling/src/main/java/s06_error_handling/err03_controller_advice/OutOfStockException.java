package s06_error_handling.err03_controller_advice;

/** Wyjątek domenowy: brak towaru w magazynie. */
public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String productName) {
        super("Brak towaru: " + productName);
    }
}
