package s06_error_handling.err05_custom_exceptions;

/**
 * Bramka płatności (system zewnętrzny) nie odpowiada. Komunikat jest bezpieczny dla klienta; prawdziwa przyczyna
 * (np. "Connection refused: pay.internal:8443") siedzi w {@code cause} i trafia tylko do logu — wyjątek opakowuje
 * (wrapping) niskopoziomowy błąd, jak w t10_exceptions/Exceptions06ChainingWrapping.
 */
public class PaymentGatewayException extends ShopException {

    public PaymentGatewayException(Throwable cause) {
        super(ErrorCode.PAYMENT_UNAVAILABLE, "Płatność chwilowo niedostępna. Spróbuj ponownie później.", cause);
    }
}
