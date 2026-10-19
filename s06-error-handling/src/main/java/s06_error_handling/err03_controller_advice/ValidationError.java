package s06_error_handling.err03_controller_advice;

import java.util.Map;

/** Ciało błędu walidacji: kod, komunikat ogólny i mapa „nazwa pola → komunikat” (posortowana po nazwie pola). */
public record ValidationError(String code, String message, Map<String, String> fields) {
}
