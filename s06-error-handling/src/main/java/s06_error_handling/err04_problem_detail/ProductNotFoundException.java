package s06_error_handling.err04_problem_detail;

/** Nie ma takiego produktu. Niesie ze sobą id — advice doda je jako własne pole odpowiedzi ProblemDetail. */
public class ProductNotFoundException extends RuntimeException {

    private final long productId;

    public ProductNotFoundException(long productId) {
        super("Nie znaleziono produktu o id " + productId);
        this.productId = productId;
    }

    public long getProductId() {
        return productId;
    }
}
