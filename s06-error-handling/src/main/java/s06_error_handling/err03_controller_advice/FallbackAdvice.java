package s06_error_handling.err03_controller_advice;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handler "łap wszystko" (fallback = rozwiązanie zapasowe). {@code @Order(LOWEST_PRECEDENCE)} jest OBOWIĄZKOWE: między
 * różnymi advice wygrywa ten z wyższym pierwszeństwem, więc "łapacz wszystkiego" musi być ostatni (sekcja 4).
 * <p>
 * Uwaga: gdyby poniższy handler zwracał zawsze 500, połknąłby też wyjątki, które Spring sam umie zamienić na 4xx
 * (405, 415, 404, 400…). Dlatego najpierw je wyłapujemy i oddajemy z ich własnym statusem.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class FallbackAdvice {

    private static final Logger log = LoggerFactory.getLogger(FallbackAdvice.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> onAnything(Exception exception, HttpServletRequest request) {
        // ErrorResponse = interfejs wyjątków Springa, które same znają swój status HTTP (405, 415, 404, 400…).
        if (exception instanceof ErrorResponse errorResponse) {
            int status = errorResponse.getStatusCode().value();
            ApiError body = new ApiError(status, "HTTP_" + status, errorResponse.getBody().getDetail(), request.getRequestURI());
            return ResponseEntity.status(errorResponse.getStatusCode()).headers(errorResponse.getHeaders()).body(body);
        }
        // Zepsuty JSON i zły typ parametru (np. /products/abc zamiast liczby) to 400, ale te wyjątki NIE są ErrorResponse.
        if (exception instanceof HttpMessageNotReadableException || exception instanceof TypeMismatchException) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(400, "MALFORMED_REQUEST", "Nie można odczytać żądania", request.getRequestURI()));
        }
        log.error("Nieoczekiwany błąd w {}", request.getRequestURI(), exception);   // szczegóły tylko w logu serwera
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(500, "UNEXPECTED", "Wystąpił nieoczekiwany błąd", request.getRequestURI()));
    }
}
