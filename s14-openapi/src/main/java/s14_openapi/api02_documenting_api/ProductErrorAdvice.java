package s14_openapi.api02_documenting_api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Globalna obsługa błędów kontrolerów (szczegóły: s06_error_handling). Dziedziczenie po
 * {@code ResponseEntityExceptionHandler} sprawia, że błędy Spring MVC (np. nieprawidłowe ciało zapytania, walidacja
 * {@code @Valid}) dostają ciało w formacie ProblemDetail (application/problem+json). Tu dokładamy obsługę 404.
 */
@RestControllerAdvice
public class ProductErrorAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail notFound(ProductNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
