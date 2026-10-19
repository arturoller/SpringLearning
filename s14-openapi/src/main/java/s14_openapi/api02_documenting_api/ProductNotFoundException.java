package s14_openapi.api02_documenting_api;

/** Wyjątek „nie ma takiego produktu” — zamieniany na ProblemDetail ze statusem 404 w ProductErrorAdvice. */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String sku) {
        super("Nie ma produktu " + sku);
    }
}
