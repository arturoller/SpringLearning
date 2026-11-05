package s06_error_handling.err05_custom_exceptions;

/** Wartość zamówienia jest za niska (reguła biznesowa bez konfliktu ze stanem → 422). */
public class MinimumOrderValueException extends BusinessRuleException {

    public MinimumOrderValueException(int minimumQuantity) {
        super("MINIMUM_ORDER_VALUE", "Minimalna ilość w zamówieniu to " + minimumQuantity);
    }
}
