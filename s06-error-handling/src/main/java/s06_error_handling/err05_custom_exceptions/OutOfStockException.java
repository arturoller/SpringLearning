package s06_error_handling.err05_custom_exceptions;

/** Brak towaru w magazynie (konflikt ze stanem magazynu). */
public class OutOfStockException extends BusinessRuleException {

    public OutOfStockException(String productName, int requested, int available) {
        super("OUT_OF_STOCK", "Brak towaru: " + productName + " (chcesz " + requested + ", jest " + available + ")");
    }
}
