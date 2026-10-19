package s06_error_handling.err05_custom_exceptions;

import java.util.Map;

/** Za mało towaru w magazynie. Zapytanie jest poprawne, ale koliduje z aktualnym stanem zasobu → 409 (konflikt). */
public final class OutOfStockException extends ShopException {

    private final String sku;
    private final int available;

    public OutOfStockException(String sku, int available, int requested) {
        super("BRAK_TOWARU", "Brak towaru " + sku + ": dostępne " + available + ", żądane " + requested);
        this.sku = sku;
        this.available = available;
    }

    @Override
    public Map<String, Object> details() {
        return Map.of("sku", sku, "available", available);
    }
}
