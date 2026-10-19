package s13_rest_client.rc02_errors_and_timeouts;

/**
 * Wyjątek domenowy: „dostawca chwilowo nie działa” — jego serwer odpowiedział 5xx, nie odpowiedział w limicie czasu
 * albo nie dało się z nim połączyć. Dla sklepu to jedna sytuacja: spróbuj później (ponawianie — lekcja Rc04).
 * Oryginalny wyjątek HTTP trafia do przyczyny (cause) — nie gubimy informacji (t10_exceptions/Exceptions06ChainingWrapping).
 */
public class SupplierUnavailableException extends RuntimeException {

    public SupplierUnavailableException(String message) {
        super(message);
    }

    public SupplierUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
