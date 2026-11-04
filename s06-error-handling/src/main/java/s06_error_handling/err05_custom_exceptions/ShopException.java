package s06_error_handling.err05_custom_exceptions;

/**
 * Korzeń hierarchii wyjątków sklepu. {@code abstract} (abstract = abstrakcyjna, nie da się utworzyć obiektu wprost — zawsze
 * rzucamy konkretny wyjątek), {@code RuntimeException} (unchecked = niekontrolowany, kompilator nie wymusza {@code throws}
 * ani {@code try/catch}). Każdy wyjątek niesie stały kod biznesowy ({@code code}), np. {@code OUT_OF_STOCK}, który trafia
 * do klienta API. Klasa NIE zna HTTP — nie ma tu statusów ani adnotacji Springa.
 */
public abstract class ShopException extends RuntimeException {

    private final String code;

    protected ShopException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Stały, maszynowy kod błędu (nie zmienia się przy poprawianiu tekstów). */
    public String getCode() {
        return code;
    }
}
