package s06_error_handling.err04_problem_detail;

/** Brak produktu — zwykły wyjątek domeny (RuntimeException). Zamianę na ProblemDetail robi ProblemAdvice. */
public class ProductNotFoundException extends RuntimeException {

    private final String sku;

    public ProductNotFoundException(String sku) {
        super("Nie ma produktu o SKU " + sku);
        this.sku = sku;
    }

    public String getSku() {
        return sku;
    }
}
