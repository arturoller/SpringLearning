package s06_error_handling.err02_exception_handler;

/** Zwykły wyjątek domenowy, bez adnotacji — jego kod HTTP wybiera dopiero metoda {@code @ExceptionHandler}. */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(long id) {
        super("Nie znaleziono zamówienia o id " + id);
    }
}
