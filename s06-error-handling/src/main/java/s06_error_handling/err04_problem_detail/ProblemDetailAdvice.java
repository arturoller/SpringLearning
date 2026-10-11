package s06_error_handling.err04_problem_detail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/**
 * Własny advice lekcji. Dziedziczy po {@code ResponseEntityExceptionHandler} (klasa bazowa Springa, która zamienia
 * WSZYSTKIE standardowe wyjątki Spring MVC na ProblemDetail), a do tego dopisuje handler wyjątku domenowego.
 * Włączany właściwością {@code err04.custom-advice=true} ({@code @ConditionalOnProperty} = warunek: tylko gdy właściwość ma wartość).
 */
@RestControllerAdvice
@ConditionalOnProperty(prefix = "err04", name = "custom-advice", havingValue = "true")
public class ProblemDetailAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail productNotFound(ProductNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Produkt nie istnieje");
        problem.setType(URI.create("https://shop.example.com/problems/product-not-found"));
        problem.setProperty("productId", e.getProductId());   // własne pole — w JSON-ie pojawi się na głównym poziomie
        return problem;
    }
}
