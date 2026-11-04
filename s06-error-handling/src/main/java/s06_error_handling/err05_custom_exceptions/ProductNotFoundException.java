package s06_error_handling.err05_custom_exceptions;

/** Brak produktu o podanym id. */
public class ProductNotFoundException extends NotFoundException {

    public ProductNotFoundException(long productId) {
        super("PRODUCT_NOT_FOUND", "Nie ma produktu o id " + productId);
    }
}
