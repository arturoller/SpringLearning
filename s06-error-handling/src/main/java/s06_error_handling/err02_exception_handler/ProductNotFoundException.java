package s06_error_handling.err02_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Brak produktu o podanym SKU. {@code @ResponseStatus} (status odpowiedzi) na klasie wyjątku: gdy wyjątek wyleci
 * z kontrolera i nikt go nie obsłuży, Spring odpowie kodem 404.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String sku) {
        super("Nie ma produktu o SKU " + sku);
    }
}
