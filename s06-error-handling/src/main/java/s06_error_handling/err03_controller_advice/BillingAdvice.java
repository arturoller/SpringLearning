package s06_error_handling.err03_controller_advice;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Wąska porada tylko dla BillingController: {@code assignableTypes} (typy, do których można przypisać) wskazuje klasy
 * kontrolerów. Obsługuje ten sam wyjątek co porada globalna (IllegalStateException), ale inaczej — i wygrywa tam, gdzie
 * obie pasują, bo ma wyższy priorytet ({@code @Order(HIGHEST_PRECEDENCE)}: im niższa liczba, tym ważniejsza porada).
 */
@RestControllerAdvice(assignableTypes = BillingController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BillingAdvice {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiError onGatewayDown(IllegalStateException e) {
        return new ApiError("BRAMKA_PLATNOSCI", "Bramka płatności jest niedostępna");
    }
}
