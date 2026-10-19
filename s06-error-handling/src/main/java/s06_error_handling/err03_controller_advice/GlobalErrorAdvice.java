package s06_error_handling.err03_controller_advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.TreeMap;

/**
 * Globalna porada (advice = rada, porada): handlery wspólne dla WSZYSTKICH kontrolerów aplikacji.
 * {@code @RestControllerAdvice} = {@code @ControllerAdvice} + {@code @ResponseBody} (wyniki handlerów trafiają do ciała JSON).
 * {@code @Order(LOWEST_PRECEDENCE)} — ta porada jest „siatką bezpieczeństwa”, sprawdzaną jako ostatnia.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalErrorAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorAdvice.class);

    /** Brak produktu → 404 i nasze ciało. */
    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError onNotFound(ProductNotFoundException e) {
        return new ApiError("PRODUKT_NIE_ISTNIEJE", e.getMessage());
    }

    /** Walidacja {@code @Valid @RequestBody} → 400 i mapa pole → komunikat (bez zależności od języka systemu). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationError onInvalid(MethodArgumentNotValidException e) {
        Map<String, String> fields = new TreeMap<>();   // TreeMap = posortowane po nazwie pola, więc wynik jest powtarzalny
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fields.merge(error.getField(), String.valueOf(error.getDefaultMessage()), (a, b) -> a.compareTo(b) <= 0 ? a + "; " + b : b + "; " + a);
        }
        return new ValidationError("BLAD_WALIDACJI", "Dane zamówienia są niepoprawne", fields);
    }

    /** Niepoprawny JSON w ciele zapytania → 400 (bez szczegółów parsera). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onUnreadable(HttpMessageNotReadableException e) {
        return new ApiError("NIEPOPRAWNY_JSON", "Ciało zapytania nie jest poprawnym JSON-em");
    }

    /**
     * Błąd wewnętrzny → 500, ale klient dostaje ogólny komunikat, a szczegóły ze stosem idą do logu — RAZ i w jednym miejscu.
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError onInternal(IllegalStateException e) {
        log.error("[BLAD-WEWNETRZNY] Nieobsłużony błąd w aplikacji", e);
        return new ApiError("BLAD_WEWNETRZNY", "Wystąpił błąd wewnętrzny. Spróbuj ponownie później.");
    }
}
