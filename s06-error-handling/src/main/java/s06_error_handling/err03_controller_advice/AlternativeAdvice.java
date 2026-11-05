package s06_error_handling.err03_controller_advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Advice nr 2 dla {@link PriorityDemoException}: {@code @Order(2)}, więc przegrywa z {@link PrimaryAdvice}. Alfabetycznie
 * jest PRZED nim — dowód, że o zwycięstwie decyduje {@code @Order}, a nie nazwa klasy.
 */
@RestControllerAdvice
@Order(2)
public class AlternativeAdvice {

    @ExceptionHandler(PriorityDemoException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError onPriorityDemo(PriorityDemoException exception, HttpServletRequest request) {
        return new ApiError(422, "ALTERNATIVE", exception.getMessage(), request.getRequestURI());
    }
}
