package s06_error_handling.err04_problem_detail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Globalna obsługa błędów lekcji w standardzie RFC 9457. Dziedziczy po {@code ResponseEntityExceptionHandler} (response entity
 * exception handler = klasa bazowa z gotową obsługą wyjątków Springa), więc 405, 415, 404, 400 (zepsuty JSON, zły typ
 * parametru), brak parametru itd. dostają {@code application/problem+json} bez ani jednej naszej linijki.
 * My dopisujemy tylko to, co jest specyficzne dla sklepu: wyjątki domenowe oraz bogatszy błąd walidacji.
 */
@RestControllerAdvice
public class Err04Advice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(Err04Advice.class);

    /** Adres "dokumentacji błędu": identyfikuje RODZAJ błędu (nie konkretne zdarzenie). W prawdziwym API powinien istnieć. */
    private static final String TYPE_BASE = "https://sklep.example/errors/";

    // --- sekcja 2: handler zwraca ProblemDetail --------------------------------------------------------------------

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail onProductNotFound(ProductNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create(TYPE_BASE + "product-not-found"));
        problem.setTitle("Produkt nie istnieje");
        problem.setProperty("code", "PRODUCT_NOT_FOUND");   // własna właściwość (rozszerzenie RFC 9457)
        return problem;
    }

    @ExceptionHandler(OutOfStockException.class)
    public ProblemDetail onOutOfStock(OutOfStockException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create(TYPE_BASE + "out-of-stock"));
        problem.setTitle("Brak towaru");
        problem.setProperty("code", "OUT_OF_STOCK");
        return problem;
    }

    // --- sekcja 4: nadpisanie metody z klasy bazowej ----------------------------------------------------------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))   // stała kolejność odpowiedzi
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Dane są niepoprawne");
        problem.setType(URI.create(TYPE_BASE + "validation-failed"));
        problem.setTitle("Błąd walidacji");
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    // --- sekcja 4: "łap wszystko" bez psucia wyjątków Springa ---------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ProblemDetail onAnything(Exception exception) {
        log.error("Nieoczekiwany błąd", exception);   // szczegóły tylko w logu serwera
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Wystąpił nieoczekiwany błąd");
        problem.setProperty("code", "UNEXPECTED");
        return problem;
    }
}
