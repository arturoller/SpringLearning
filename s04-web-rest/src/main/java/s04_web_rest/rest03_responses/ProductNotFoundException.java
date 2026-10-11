package s04_web_rest.rest03_responses;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ProductNotFoundException = wyjątek „nie znaleziono produktu”. {@code @ResponseStatus(HttpStatus.NOT_FOUND)} na klasie
 * wyjątku: gdy metoda kontrolera go rzuci, Spring odpowie 404 zamiast 500 (robi to ResponseStatusExceptionResolver).
 * Pełna obsługa błędów (ProblemDetail, {@code @RestControllerAdvice}) — dział s06_error_handling.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("Nie ma produktu o id " + id);
    }
}
