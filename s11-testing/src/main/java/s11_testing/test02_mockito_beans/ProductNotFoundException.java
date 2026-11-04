package s11_testing.test02_mockito_beans;

/** Nie ma produktu o takim numerze (na 404 zamienia go {@link ProductErrorAdvice}). */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("Nie ma produktu o numerze " + id);
    }
}
