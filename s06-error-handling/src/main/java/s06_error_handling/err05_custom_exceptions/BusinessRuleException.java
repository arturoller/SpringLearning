package s06_error_handling.err05_custom_exceptions;

/**
 * Gałąź hierarchii: "żądanie jest poprawne technicznie, ale łamie regułę biznesową". Domyślnie zamienimy ją na 422; dwa
 * konkretne wyjątki (brak towaru, zamówienie już wysłane) to konflikt ze stanem systemu, więc dostaną 409.
 */
public abstract class BusinessRuleException extends ShopException {

    protected BusinessRuleException(String code, String message) {
        super(code, message);
    }
}
