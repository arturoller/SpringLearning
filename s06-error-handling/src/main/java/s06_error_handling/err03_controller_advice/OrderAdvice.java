package s06_error_handling.err03_controller_advice;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Advice OGRANICZONY do jednego kontrolera: {@code assignableTypes = OrderController.class}. Ma najwyższy priorytet
 * ({@code @Order(1)}), a jego handler RuntimeException jest bardzo szeroki — dla OrderController przesłania więc
 * nawet bardziej szczegółowe handlery z advice o niższym priorytecie.
 */
@RestControllerAdvice(assignableTypes = OrderController.class)
@Order(1)
public class OrderAdvice {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflict(IllegalStateException e) {
        return Map.of("handledBy", "order-advice", "error", e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> anyRuntime(RuntimeException e) {
        return Map.of("handledBy", "order-advice-fallback", "error", "Obsługa zamówień chwilowo niedostępna");
    }
}
