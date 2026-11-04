package s06_error_handling.err03_controller_advice;

/** Wyjątek domenowy sklepu (nie wie nic o HTTP — kod odpowiedzi wybiera advice). */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long productId) {
        super("Nie ma produktu o id " + productId);
    }
}
