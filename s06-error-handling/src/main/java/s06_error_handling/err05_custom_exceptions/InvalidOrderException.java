package s06_error_handling.err05_custom_exceptions;

/**
 * Zamówienie jest poprawne składniowo (JSON się wczytał, pola mają dobre typy), ale łamie regułę biznesową
 * (np. pusta lista pozycji, nieznany produkt w pozycji) → 422 Unprocessable Content („nie do przetworzenia”).
 */
public final class InvalidOrderException extends ShopException {

    public InvalidOrderException(String message) {
        super("ZAMOWIENIE_NIEPOPRAWNE", message);
    }
}
