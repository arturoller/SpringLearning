package s06_error_handling.err03_controller_advice;

/** Nie ma takiego produktu. Zwykły wyjątek domenowy (bez @ResponseStatus) — kod HTTP wybiera dopiero advice. */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("Nie znaleziono produktu o id " + id);
    }
}
