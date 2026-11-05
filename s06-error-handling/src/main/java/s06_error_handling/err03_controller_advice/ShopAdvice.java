package s06_error_handling.err03_controller_advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.TreeMap;

/**
 * Globalna obsługa błędów sklepu: jedno miejsce dla WSZYSTKICH kontrolerów. {@code @RestControllerAdvice} =
 * {@code @ControllerAdvice} + {@code @ResponseBody} (advice = "porada, wskazówka" — klasa, która "doradza" wszystkim kontrolerom;
 * response body = ciało odpowiedzi: wartości zwracane są zamieniane na JSON).
 */
@RestControllerAdvice
@Order(100)
public class ShopAdvice {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError onProductNotFound(ProductNotFoundException exception, HttpServletRequest request) {
        return new ApiError(404, "PRODUCT_NOT_FOUND", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError onOrderNotFound(OrderNotFoundException exception, HttpServletRequest request) {
        return new ApiError(404, "ORDER_NOT_FOUND", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(OutOfStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError onOutOfStock(OutOfStockException exception, HttpServletRequest request) {
        return new ApiError(409, "OUT_OF_STOCK", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onInvalidBody(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new TreeMap<>();   // posortowane: odpowiedź zawsze w tej samej kolejności
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.merge(error.getField(), String.valueOf(error.getDefaultMessage()), (first, second) -> first + "; " + second);
        }
        return new ApiError(400, "VALIDATION_FAILED", "Dane są niepoprawne", request.getRequestURI(), fields);
    }
}
