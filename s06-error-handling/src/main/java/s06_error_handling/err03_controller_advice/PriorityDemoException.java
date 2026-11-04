package s06_error_handling.err03_controller_advice;

/** Wyjątek obsługiwany przez DWA advice naraz — o zwycięzcy decyduje {@code @Order} (sekcja 2). */
public class PriorityDemoException extends RuntimeException {

    public PriorityDemoException() {
        super("Wyjątek do demonstracji kolejności advice");
    }
}
