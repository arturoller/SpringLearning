package s11_testing.test01_test_slices;

/** Wyjątek domenowy: nie ma produktu o takim numerze. Na kod 404 zamienia go {@link ProductErrorAdvice}. */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("Nie ma produktu o numerze " + id);
    }
}
