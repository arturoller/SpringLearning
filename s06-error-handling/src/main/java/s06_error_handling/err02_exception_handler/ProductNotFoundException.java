package s06_error_handling.err02_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Wyjątek oznaczony {@code @ResponseStatus} (response status = status odpowiedzi): gdy wyleci z kontrolera, Spring
 * odpowie kodem 404 i podanym powodem (reason) — bez pisania żadnej metody obsługi.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Nie ma takiego produktu")
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("Nie znaleziono produktu o id " + id);
    }
}
