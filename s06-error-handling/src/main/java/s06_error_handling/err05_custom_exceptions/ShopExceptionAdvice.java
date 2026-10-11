package s06_error_handling.err05_custom_exceptions;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * JEDNO miejsce, które zamienia wyjątki na odpowiedzi HTTP (ProblemDetail) i JEDNO miejsce, które loguje błędy serwera.
 * Dziedziczy po ResponseEntityExceptionHandler, więc wyjątki Springa MVC (405, brak parametru...) zachowują poprawne kody.
 */
@RestControllerAdvice
public class ShopExceptionAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ShopExceptionAdvice.class);
    private static final String TYPE_BASE = "https://shop.example.com/problems/";

    /** Wyjątki domenowe: kod błędu → status HTTP. Switch po enum jest wyczerpujący: nowy kod = błąd kompilacji, dopóki go nie obsłużysz. */
    private static HttpStatus statusOf(ErrorCode code) {
        return switch (code) {
            case PRODUCT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case OUT_OF_STOCK -> HttpStatus.CONFLICT;
            case PAYMENT_UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
        };
    }

    private static String slugOf(ErrorCode code) {
        return code.name().toLowerCase().replace('_', '-');
    }

    private static String titleOf(ErrorCode code) {
        return switch (code) {
            case PRODUCT_NOT_FOUND -> "Produkt nie istnieje";
            case OUT_OF_STOCK -> "Brak towaru";
            case PAYMENT_UNAVAILABLE -> "Płatność niedostępna";
        };
    }

    @ExceptionHandler(ShopException.class)
    public ProblemDetail shopException(ShopException e) {
        HttpStatus status = statusOf(e.getCode());
        if (status.is5xxServerError()) {
            log.error("Błąd po stronie serwera: {}", e.getMessage(), e);   // raz, z całym łańcuchem przyczyn
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());   // komunikat ShopException jest bezpieczny
        problem.setTitle(titleOf(e.getCode()));
        problem.setType(URI.create(TYPE_BASE + slugOf(e.getCode())));
        problem.setProperty("code", e.getCode().name());
        if (e instanceof OutOfStockException stock) {   // dopasowanie wzorca: dodatkowe pola tylko dla tego wyjątku
            problem.setProperty("requested", stock.getRequested());
            problem.setProperty("available", stock.getAvailable());
        }
        return problem;
    }

    /** Walidacja {@code @Valid}: 400 i mapa "pole → lista komunikatów" (posortowana, więc deterministyczna). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, List<String>> errors = new TreeMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(fieldError.getField(), field -> new ArrayList<>()).add(fieldError.getDefaultMessage());
        }
        errors.values().forEach(messages -> messages.sort(String::compareTo));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dane zamówienia są niepoprawne");
        problem.setTitle("Niepoprawne dane");
        problem.setType(URI.create(TYPE_BASE + "validation-failed"));
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Siatka bezpieczeństwa: nieprzewidziany błąd → 500, stały komunikat, szczegóły TYLKO w logu (raz). */
    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception e) {
        log.error("Nieobsłużony błąd", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Wystąpił nieoczekiwany błąd");
        problem.setTitle("Błąd serwera");
        problem.setType(URI.create(TYPE_BASE + "internal-error"));
        return problem;
    }
}
