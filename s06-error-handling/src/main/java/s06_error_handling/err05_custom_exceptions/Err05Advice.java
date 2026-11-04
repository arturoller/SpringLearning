package s06_error_handling.err05_custom_exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Locale;

/**
 * JEDNO miejsce, w którym wyjątki domenowe zamieniają się w odpowiedzi HTTP. Tabela mapowania (wyjątek → status):
 * <pre>
 *   NotFoundException (ProductNotFound, OrderNotFound)         → 404 Not Found
 *   OutOfStockException, OrderAlreadyShippedException           → 409 Conflict
 *   inne BusinessRuleException (MinimumOrderValue)              → 422 Unprocessable Content
 *   każdy inny wyjątek (nieoczekiwany)                          → 500 Internal Server Error
 * </pre>
 * W obrębie jednego advice wygrywa NAJBLIŻSZY typ wyjątku, dlatego {@code OutOfStockException} (409) wyprzedza swoją klasę
 * bazową {@code BusinessRuleException} (422). Wyjątki Springa (405, 415, 400…) obsługuje klasa bazowa
 * {@code ResponseEntityExceptionHandler} (Err04ProblemDetail).
 */
@RestControllerAdvice
public class Err05Advice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(Err05Advice.class);

    private static final String TYPE_BASE = "https://sklep.example/errors/";

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail onNotFound(NotFoundException exception) {
        return clientError(exception, HttpStatus.NOT_FOUND, "Nie znaleziono zasobu");
    }

    @ExceptionHandler({OutOfStockException.class, OrderAlreadyShippedException.class})
    public ProblemDetail onConflict(BusinessRuleException exception) {
        return clientError(exception, HttpStatus.CONFLICT, "Konflikt ze stanem sklepu");
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail onBusinessRule(BusinessRuleException exception) {
        return clientError(exception, HttpStatus.UNPROCESSABLE_CONTENT, "Naruszona reguła biznesowa");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail onUnexpected(Exception exception) {
        // 5xx = NASZ błąd: poziom ERROR i PEŁNY ślad stosu (wyjątek jako ostatni argument). Klient dostaje tylko ogólny opis.
        log.error("Nieoczekiwany błąd serwera", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Wystąpił nieoczekiwany błąd");
        problem.setProperty("code", "UNEXPECTED");
        return problem;
    }

    /** Wspólne składanie ProblemDetail z wyjątku domenowego: kod biznesowy trafia do właściwości {@code code} i do adresu {@code type}. */
    private ProblemDetail clientError(ShopException exception, HttpStatus status, String title) {
        // 4xx = błąd KLIENTA, spodziewany w normalnej pracy API: poziom INFO, jedna linia, BEZ śladu stosu.
        log.info("{} {}: {}", status.value(), exception.getCode(), exception.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(title);
        problem.setType(URI.create(TYPE_BASE + exception.getCode().toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setProperty("code", exception.getCode());
        return problem;
    }
}
