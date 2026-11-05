package s11_testing.test01_test_slices;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Wspólna obsługa błędów kontrolerów (advice = rada, rozszerzenie działające dla wielu kontrolerów).
 * Jest częścią warstwy webowej, więc {@code @WebMvcTest} ładuje ją razem z kontrolerem.
 */
@RestControllerAdvice
public class ProductErrorAdvice {

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail notFound(ProductNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Nie ma takiego produktu");
        return problem;
    }
}
