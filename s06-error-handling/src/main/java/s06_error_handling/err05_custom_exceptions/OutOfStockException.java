package s06_error_handling.err05_custom_exceptions;

/** Za mało towaru w magazynie. Niesie liczby (zamówione, dostępne) jako pola — advice dołączy je do odpowiedzi. */
public class OutOfStockException extends ShopException {

    private final long productId;
    private final int requested;
    private final int available;

    public OutOfStockException(long productId, int requested, int available) {
        super(ErrorCode.OUT_OF_STOCK,
                "Za mało towaru (produkt " + productId + "): zamówiono " + requested + ", dostępne " + available);
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public long getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
