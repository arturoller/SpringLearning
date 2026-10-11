package s10_transactions.tx01_transactional;

/**
 * Bramka płatności (payment gateway — zewnętrzny system banku) nie odpowiada. Wyjątek KONTROLOWANY (checked,
 * dziedziczy po Exception, a nie po RuntimeException) → domyślnie Spring NIE wycofuje transakcji!
 */
public class PaymentGatewayException extends Exception {

    public PaymentGatewayException(String message) {
        super(message);
    }
}
