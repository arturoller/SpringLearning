package s10_transactions.tx02_propagation;

/** Brak towaru na magazynie (out of stock = brak w magazynie). Niesprawdzany — domyślnie wycofuje transakcję. */
public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String product, int quantity) {
        super("Brak towaru: " + product + " ×" + quantity);
    }
}
