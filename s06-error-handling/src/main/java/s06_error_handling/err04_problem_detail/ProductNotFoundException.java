package s06_error_handling.err04_problem_detail;

/** Wyjątek domenowy sklepu (nie wie nic o HTTP — status i format odpowiedzi wybiera advice). */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long productId) {
        super("Nie ma produktu o id " + productId);
    }
}
