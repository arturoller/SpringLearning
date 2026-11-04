package s06_error_handling.solutions.err03_controller_advice;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import s06_error_handling.err03_controller_advice.ApiError;

@RestControllerAdvice(assignableTypes = {Err03SolutionsController.class, Err03SolutionsVipController.class})
@Order(Ordered.LOWEST_PRECEDENCE)
public class Err03SolutionsAdvice {

    private static final Logger log = LoggerFactory.getLogger(Err03SolutionsAdvice.class);

    @ExceptionHandler(Err03SolutionsController.CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError onCustomerNotFound(Err03SolutionsController.CustomerNotFoundException exception, HttpServletRequest request) {
        return new ApiError(404, "CUSTOMER_NOT_FOUND", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Err03SolutionsController.OrderLockedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError onOrderLocked(Err03SolutionsController.OrderLockedException exception, HttpServletRequest request) {
        return new ApiError(409, "ORDER_LOCKED", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError onAnything(Exception exception, HttpServletRequest request) {
        log.error("Nieoczekiwany błąd w {}", request.getRequestURI(), exception);
        return new ApiError(500, "UNEXPECTED", "Wystąpił nieoczekiwany błąd", request.getRequestURI());
    }
}
