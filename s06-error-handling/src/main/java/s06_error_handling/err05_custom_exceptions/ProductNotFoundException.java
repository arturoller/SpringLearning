package s06_error_handling.err05_custom_exceptions;

/** Nie ma produktu o podanym id. */
public class ProductNotFoundException extends ShopException {

    private final long productId;

    public ProductNotFoundException(long productId) {
        super(ErrorCode.PRODUCT_NOT_FOUND, "Nie znaleziono produktu o id " + productId);
        this.productId = productId;
    }

    public long getProductId() {
        return productId;
    }
}
