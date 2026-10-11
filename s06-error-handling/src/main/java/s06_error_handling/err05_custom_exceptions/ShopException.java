package s06_error_handling.err05_custom_exceptions;

/**
 * Korzeń hierarchii wyjątków sklepu. {@code abstract} (abstrakcyjna) — nie rzuca się "gołego" ShopException, tylko
 * jego konkretne podklasy. Dziedziczy po RuntimeException (wyjątek niesprawdzany, unchecked — nie trzeba go deklarować
 * ani łapać: t10_exceptions/Exceptions02CheckedUnchecked).
 *
 * <p>Umowa: {@code getMessage()} każdego ShopException jest napisany Z MYŚLĄ O KLIENCIE (po polsku, bez szczegółów
 * technicznych) i wolno go odesłać w odpowiedzi. Szczegóły wewnętrzne trzymamy wyłącznie w przyczynie ({@code cause}) —
 * ta trafia tylko do logu.</p>
 */
public abstract class ShopException extends RuntimeException {

    private final ErrorCode code;

    protected ShopException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    protected ShopException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
