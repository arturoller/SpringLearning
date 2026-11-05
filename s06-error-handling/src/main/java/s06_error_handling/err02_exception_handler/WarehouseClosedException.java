package s06_error_handling.err02_exception_handler;

/** Wyjątek domenowy: magazyn jest zamknięty (np. inwentaryzacja). */
public class WarehouseClosedException extends RuntimeException {

    public WarehouseClosedException() {
        super("Magazyn jest chwilowo zamknięty");
    }
}
