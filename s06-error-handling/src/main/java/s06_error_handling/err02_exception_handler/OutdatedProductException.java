package s06_error_handling.err02_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Wyjątek z adnotacją na 410 Gone (zasób usunięty na stałe) — ale CatalogController ma własną metodę obsługi. */
@ResponseStatus(HttpStatus.GONE)
public class OutdatedProductException extends RuntimeException {

    public OutdatedProductException(String message) {
        super(message);
    }
}
