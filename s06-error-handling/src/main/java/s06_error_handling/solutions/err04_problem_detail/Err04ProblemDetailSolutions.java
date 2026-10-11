package s06_error_handling.solutions.err04_problem_detail;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/** Rozwiązania wzorcowe ćwiczeń lekcji Err04ProblemDetail — zajrzyj dopiero po własnej próbie! */
public final class Err04ProblemDetailSolutions {

    private Err04ProblemDetailSolutions() {
    }

    static ProblemDetail solution1(long productId) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Nie znaleziono produktu o id " + productId);
        problem.setTitle("Produkt nie istnieje");
        problem.setType(URI.create("https://shop.example.com/problems/product-not-found"));
        problem.setProperty("productId", productId);
        return problem;
    }

    static ErrorResponseException solution2(int retryAfterSeconds) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Limit zapytań przekroczony");
        problem.setTitle("Zbyt wiele zapytań");
        problem.setProperty("retryAfterSeconds", retryAfterSeconds);
        return new ErrorResponseException(HttpStatus.TOO_MANY_REQUESTS, problem, null);
    }

    @RestControllerAdvice
    static class Solution3Advice extends ResponseEntityExceptionHandler {

        @ExceptionHandler(IllegalArgumentException.class)
        ProblemDetail illegalArgument(IllegalArgumentException e) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
            problem.setTitle("Niepoprawne dane");
            return problem;
        }
    }
}
