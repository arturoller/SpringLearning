package s06_error_handling.err03_controller_advice;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Advice o wyższym priorytecie ({@code @Order(2)}) niż GlobalExceptionHandler ({@code @Order(3)}): płatności to 402. */
@RestControllerAdvice
@Order(2)
public class PaymentAdvice {

    @ExceptionHandler(PaymentDeclinedException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public Map<String, String> paymentDeclined(PaymentDeclinedException e) {
        return Map.of("handledBy", "payment-advice", "error", "Płatność odrzucona przez bank");
    }
}
