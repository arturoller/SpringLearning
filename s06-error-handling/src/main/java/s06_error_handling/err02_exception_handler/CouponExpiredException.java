package s06_error_handling.err02_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Kupon wygasł. Ma {@code @ResponseStatus(GONE)}, ale kontroler kuponów obsługuje go własnym handlerem — kto wygra? */
@ResponseStatus(HttpStatus.GONE)
public class CouponExpiredException extends RuntimeException {

    public CouponExpiredException(String code) {
        super("Kupon " + code + " wygasł");
    }
}
