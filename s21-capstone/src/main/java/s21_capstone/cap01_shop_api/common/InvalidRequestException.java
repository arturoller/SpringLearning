package s21_capstone.cap01_shop_api.common;

import java.util.Map;

/**
 * Parametr zapytania ma wartość spoza dozwolonej listy (np. nieznane pole sortowania) — porada zamienia to na
 * 400 Bad Request. Błędy pól w ciele JSON-a obsługuje Bean Validation ({@code @Valid}), nie ten wyjątek.
 */
public final class InvalidRequestException extends ShopException {

    public InvalidRequestException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
