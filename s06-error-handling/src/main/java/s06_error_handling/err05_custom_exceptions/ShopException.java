package s06_error_handling.err05_custom_exceptions;

import java.util.Map;

/**
 * Korzeń hierarchii wyjątków domeny sklepu. Jest {@code sealed} (zapieczętowany): lista podklas jest zamknięta
 * ({@code permits}), więc kompilator wie, że switch po tych typach jest wyczerpujący (patrz ShopProblemAdvice).
 *
 * <p>Wyjątek NIE zna HTTP (nie ma statusu ani ProblemDetail) — opisuje, co się stało w sklepie. Zamianę na status i ciało
 * odpowiedzi robi jedna porada. Dziedziczy po RuntimeException (wyjątek niekontrolowany: nie trzeba go deklarować ani łapać).</p>
 */
public abstract sealed class ShopException extends RuntimeException
        permits ProductNotFoundException, OutOfStockException, InvalidOrderException {

    private final String code;

    protected ShopException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Stabilny kod błędu dla programów (klient może się na nim oprzeć, w przeciwieństwie do tekstu komunikatu). */
    public String code() {
        return code;
    }

    /** Dodatkowe dane błędu (np. sku) — domyślnie brak; podklasy mogą je dopisać. */
    public Map<String, Object> details() {
        return Map.of();
    }
}
