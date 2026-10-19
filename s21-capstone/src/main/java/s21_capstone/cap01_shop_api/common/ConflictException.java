package s21_capstone.cap01_shop_api.common;

import java.util.Map;

/**
 * Żądanie jest poprawne, ale kłóci się z obecnym STANEM danych (sku zajęty, niedozwolona zmiana statusu, produkt
 * użyty w zamówieniach) — porada zamienia to na 409 Conflict.
 */
public final class ConflictException extends ShopException {

    public ConflictException(String code, String message) {
        this(code, message, Map.of());
    }

    public ConflictException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
