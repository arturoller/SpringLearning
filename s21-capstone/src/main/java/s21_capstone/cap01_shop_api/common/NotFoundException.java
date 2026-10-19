package s21_capstone.cap01_shop_api.common;

import java.util.Map;

/** Nie ma zasobu (produktu, klienta, zamówienia) — porada zamienia to na 404 Not Found. */
public final class NotFoundException extends ShopException {

    public NotFoundException(String what, Object id) {
        super("NOT_FOUND", "Nie znaleziono: " + what + " " + id, Map.of("resource", what, "id", String.valueOf(id)));
    }
}
