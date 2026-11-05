package s06_error_handling.err02_exception_handler;

/** Wyjątek domenowy: nie ma produktu o podanym id. Nie wie nic o HTTP — o kodzie 404 decyduje handler. */
public class ProductNotFoundException extends RuntimeException {

    private final long productId;

    public ProductNotFoundException(long productId) {
        super("Nie ma produktu o id " + productId);
        this.productId = productId;
    }

    public long getProductId() {
        return productId;
    }
}
