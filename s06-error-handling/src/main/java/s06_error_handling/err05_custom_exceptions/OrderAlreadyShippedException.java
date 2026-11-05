package s06_error_handling.err05_custom_exceptions;

/** Zamówienie zostało już wysłane (konflikt ze stanem zamówienia). */
public class OrderAlreadyShippedException extends BusinessRuleException {

    public OrderAlreadyShippedException(long orderId) {
        super("ORDER_ALREADY_SHIPPED", "Zamówienie " + orderId + " zostało już wysłane");
    }
}
