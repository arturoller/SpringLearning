package s06_error_handling.err03_controller_advice;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Jeden format błędu dla CAŁEGO API sklepu: {@code status}, {@code code} (stały kod maszynowy), {@code message} (opis dla
 * człowieka), {@code path} oraz — tylko przy błędach walidacji — {@code fields} (mapa pole → komunikat).
 * {@code @JsonInclude(NON_NULL)} (include = dołącz, non-null = niepusty) pomija pola równe null, więc zwykły błąd nie ma {@code fields}.
 * (Adnotacje Jacksona zostały w pakiecie {@code com.fasterxml.jackson.annotation} także w Jacksonie 3.)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String code, String message, String path, Map<String, String> fields) {

    public ApiError(int status, String code, String message, String path) {
        this(status, code, message, path, null);
    }
}
