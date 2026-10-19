package s00_start.start08_debugging;

/** Własny wyjątek: produkt o danym SKU nie ma towaru w magazynie (np. ELE-002 Smartfon X — 0 sztuk). */
public class OutOfStockException extends RuntimeException {

    private final String sku;

    public OutOfStockException(String sku) {
        super("Brak towaru w magazynie: " + sku);
        this.sku = sku;
    }

    public String getSku() {
        return sku;
    }
}
