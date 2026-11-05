package s06_error_handling.err01_default_errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Wyjątek "brak towaru w magazynie". Adnotacja {@code @ResponseStatus} (response status = status odpowiedzi) mówi Springowi,
 * jaki kod HTTP ma dostać klient, gdy ten wyjątek wyleci z kontrolera: 409 Conflict (konflikt ze stanem magazynu).
 * Bez adnotacji byłoby 500.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String sku) {
        super("Brak towaru w magazynie: " + sku);
    }
}
