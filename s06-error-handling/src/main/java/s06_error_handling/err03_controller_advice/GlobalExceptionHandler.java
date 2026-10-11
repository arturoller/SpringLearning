package s06_error_handling.err03_controller_advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Globalna obsługa błędów aplikacji lekcji. {@code @RestControllerAdvice} = {@code @ControllerAdvice} +
 * {@code @ResponseBody}: metody tej klasy działają dla WSZYSTKICH kontrolerów i zwracają JSON.
 * {@code @Order(3)} — najniższy priorytet z trzech advice tej lekcji (im mniejsza liczba, tym ważniejszy).
 */
@RestControllerAdvice
@Order(3)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);   // logger = dziennik

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> productNotFound(ProductNotFoundException e) {
        return Map.of("handledBy", "global", "error", e.getMessage());
    }

    /** Ten sam wyjątek ma też handler w PaymentAdvice (@Order(2)) — tamten jest ważniejszy, więc ten nie zadziała. */
    @ExceptionHandler(PaymentDeclinedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> paymentDeclined(PaymentDeclinedException e) {
        return Map.of("handledBy", "global", "error", "Płatność odrzucona");
    }

    /** Siatka bezpieczeństwa: wszystko inne → 500, szczegóły TYLKO do logu, klient dostaje stały komunikat. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> anyException(Exception e) {
        log.error("Nieobsłużony błąd w aplikacji", e);
        return Map.of("handledBy", "global", "error", "Wystąpił nieoczekiwany błąd");
    }
}
