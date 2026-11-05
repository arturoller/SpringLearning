package s06_error_handling.err03_controller_advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Advice zawężony selektorem {@code annotations}: dotyczy kontrolerów oznaczonych {@link ReportsApi}. */
@RestControllerAdvice(annotations = ReportsApi.class)
@Order(10)
public class ReportsAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError onBadInput(IllegalArgumentException exception, HttpServletRequest request) {
        return new ApiError(422, "REPORT_BAD_INPUT", exception.getMessage(), request.getRequestURI());
    }
}
