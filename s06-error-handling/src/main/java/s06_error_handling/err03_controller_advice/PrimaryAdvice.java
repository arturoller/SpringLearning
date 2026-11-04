package s06_error_handling.err03_controller_advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Advice nr 1 dla {@link PriorityDemoException}: {@code @Order(1)} — MNIEJSZA liczba = WYŻSZE pierwszeństwo. */
@RestControllerAdvice
@Order(1)
public class PrimaryAdvice {

    @ExceptionHandler(PriorityDemoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onPriorityDemo(PriorityDemoException exception, HttpServletRequest request) {
        return new ApiError(400, "PRIMARY", exception.getMessage(), request.getRequestURI());
    }
}
