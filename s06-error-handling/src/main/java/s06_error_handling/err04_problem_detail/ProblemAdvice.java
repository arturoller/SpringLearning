package s06_error_handling.err04_problem_detail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

/**
 * Porada oparta na {@code ResponseEntityExceptionHandler} (gotowa klasa bazowa Springa): ma już handlery dla wszystkich
 * wyjątków Spring MVC (zła metoda 405, zły typ 415, brak parametru 400, niepoprawny JSON 400, brak adresu 404 itd.) i zamienia
 * je na {@code ProblemDetail}. My dodajemy tylko to, czego Spring nie zna: wyjątek domeny, własne pole w błędzie
 * walidacji i siatkę bezpieczeństwa dla reszty.
 *
 * <p>{@code @ConditionalOnProperty}: porada działa domyślnie; testy wyłączają ją ustawieniem
 * {@code err04.own-advice=false}, żeby pokazać wbudowaną obsługę Boota ({@code spring.mvc.problemdetails.enabled=true}).</p>
 */
@RestControllerAdvice
@ConditionalOnProperty(prefix = "err04", name = "own-advice", havingValue = "true", matchIfMissing = true)
public class ProblemAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemAdvice.class);

    /** Zwracamy ProblemDetail wprost: Spring ustawi status z pola status i typ treści application/problem+json. */
    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail onNotFound(ProductNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Produkt nie istnieje");
        problem.setType(URI.create("https://sklep.example.test/problemy/produkt-nie-istnieje"));
        problem.setProperty("sku", e.getSku());
        return problem;
    }

    /** Nadpisujemy handler z klasy bazowej, żeby do standardowego ProblemDetail dodać mapę błędów pól. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), String.valueOf(error.getDefaultMessage()));
        }
        ProblemDetail problem = ex.getBody();   // ProblemDetail, który Spring już przygotował (status 400, detail)
        problem.setTitle("Niepoprawne dane");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** Siatka bezpieczeństwa: nieznany wyjątek → 500 bez szczegółów, szczegóły do logu. Wyjątki Springa obsługuje klasa bazowa. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail onUnexpected(Exception e) {
        log.error("[BLAD-WEWNETRZNY] Nieobsłużony błąd w aplikacji", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Wystąpił błąd wewnętrzny. Spróbuj ponownie później.");
        problem.setTitle("Błąd wewnętrzny");
        return problem;
    }
}
