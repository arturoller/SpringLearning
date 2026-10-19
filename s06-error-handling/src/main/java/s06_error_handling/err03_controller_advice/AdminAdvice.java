package s06_error_handling.err03_controller_advice;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Porada zawężona do PAKIETU: {@code basePackages} obejmuje kontrolery z pakietu admin (i jego podpakietów).
 * {@code @Order(1)}: ważniejsza niż globalna, mniej ważna niż BillingAdvice (HIGHEST_PRECEDENCE to najmniejsza liczba).
 */
@RestControllerAdvice(basePackages = "s06_error_handling.err03_controller_advice.admin")
@Order(1)
public class AdminAdvice {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError onReportDisabled(IllegalStateException e) {
        return new ApiError("RAPORT_NIEDOSTEPNY", "Raporty są chwilowo niedostępne");
    }
}
