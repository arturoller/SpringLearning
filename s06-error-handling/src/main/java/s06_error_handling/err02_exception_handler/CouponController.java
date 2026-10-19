package s06_error_handling.err02_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wyjątek ma {@code @ResponseStatus(GONE)} (410), a kontroler własny handler zwracający 409. Wygrywa handler:
 * {@code @ExceptionHandler} jest sprawdzany PRZED {@code @ResponseStatus} z klasy wyjątku.
 */
@RestController
public class CouponController {

    @GetMapping("/api/coupons/{code}")
    public String coupon(@PathVariable String code) {
        throw new CouponExpiredException(code);
    }

    @ExceptionHandler(CouponExpiredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorBody onExpired(CouponExpiredException e) {
        return new ErrorBody("KUPON_WYGASL", e.getMessage());
    }
}
