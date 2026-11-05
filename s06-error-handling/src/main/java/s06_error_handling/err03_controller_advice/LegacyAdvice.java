package s06_error_handling.err03_controller_advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import s06_error_handling.err03_controller_advice.legacy.LegacyController;

/**
 * Advice zawężony selektorem {@code basePackageClasses}: dotyczy kontrolerów z pakietu, w którym leży {@link LegacyController}.
 * (Odpowiednik z tekstem: {@code basePackages = "s06_error_handling.err03_controller_advice.legacy"}; wersja z klasą nie
 * psuje się, gdy zmienisz nazwę pakietu.)
 */
@RestControllerAdvice(basePackageClasses = LegacyController.class)
@Order(10)
public class LegacyAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onBadInput(IllegalArgumentException exception, HttpServletRequest request) {
        return new ApiError(400, "LEGACY_BAD_INPUT", exception.getMessage(), request.getRequestURI());
    }
}
