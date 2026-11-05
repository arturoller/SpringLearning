package s11_testing.test03_mock_mvc_tester;

/** Nie ma produktu o takim numerze (na 404 zamienia go {@link ProductErrorAdvice}). */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("Nie ma produktu o numerze " + id);
    }
}
