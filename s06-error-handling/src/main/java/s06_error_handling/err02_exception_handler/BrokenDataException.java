package s06_error_handling.err02_exception_handler;

/** Wyjątek, dla którego handler sam się wywraca (sekcja 5: wyjątek rzucony w handlerze). */
public class BrokenDataException extends RuntimeException {

    public BrokenDataException() {
        super("Uszkodzone dane produktu");
    }
}
