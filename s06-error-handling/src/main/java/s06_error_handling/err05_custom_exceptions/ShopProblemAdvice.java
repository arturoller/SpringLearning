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
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * JEDNO miejsce, w którym wyjątki sklepu zamieniają się w odpowiedzi HTTP i wpisy w logu.
 * Zasady: błąd klienta (4xx) — krótki wpis bez stosu; błąd serwera (5xx) — wpis ze stosem i identyfikatorem błędu,
 * a klient dostaje ogólny opis plus TEN SAM identyfikator (by mógł go podać obsłudze).
 */
@RestControllerAdvice
public class ShopProblemAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ShopProblemAdvice.class);
    private static final String TYPE_BASE = "https://sklep.example.test/problemy/";

    /** Status HTTP i stały tytuł dla danego rodzaju błędu domeny. */
    private record Mapping(HttpStatus status, String title) {
    }

    private final ErrorIdGenerator errorIds;

    public ShopProblemAdvice(ErrorIdGenerator errorIds) {
        this.errorIds = errorIds;
    }

    /** Wyjątki domeny: status wybiera switch po typie (sealed → kompilator pilnuje, by nie zabrakło żadnego przypadku). */
    @ExceptionHandler(ShopException.class)
    public ProblemDetail onShopException(ShopException e) {
        Mapping mapping = switch (e) {
            case ProductNotFoundException p -> new Mapping(HttpStatus.NOT_FOUND, "Nie znaleziono");                // 404: nie ma zasobu spod adresu
            case OutOfStockException o -> new Mapping(HttpStatus.CONFLICT, "Konflikt ze stanem magazynu");         // 409: konflikt ze stanem zasobu
            case InvalidOrderException i -> new Mapping(HttpStatus.UNPROCESSABLE_CONTENT, "Niepoprawne zamówienie"); // 422: poprawna składnia, zły sens
        };
        log.info("Błąd klienta [{}]: {}", e.code(), e.getMessage());   // bez stosu: to nie awaria aplikacji
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(mapping.status(), e.getMessage());
        problem.setTitle(mapping.title());
        problem.setType(URI.create(TYPE_BASE + e.code().toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setProperty("code", e.code());
        e.details().forEach(problem::setProperty);
        return problem;
    }

    /** Awaria infrastruktury → 503; przyczyna (cause) trafia do logu razem ze stosem. */
    @ExceptionHandler(StorageUnavailableException.class)
    public ProblemDetail onStorage(StorageUnavailableException e) {
        String errorId = errorIds.next();
        log.error("[BLAD-WEWNETRZNY] errorId={} — magazyn danych niedostępny", errorId, e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Usługa jest chwilowo niedostępna. Spróbuj ponownie później.");
        problem.setTitle("Usługa niedostępna");
        problem.setProperty("errorId", errorId);
        return problem;
    }

    /** Wszystko inne (błąd programisty) → 500 bez szczegółów, ze stosem i identyfikatorem w logu. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail onUnexpected(Exception e) {
        String errorId = errorIds.next();
        log.error("[BLAD-WEWNETRZNY] errorId={} — nieoczekiwany błąd", errorId, e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Wystąpił błąd wewnętrzny. Podaj identyfikator błędu obsłudze sklepu.");
        problem.setTitle("Błąd wewnętrzny");
        problem.setProperty("errorId", errorId);
        return problem;
    }

    /** Błąd FORMATU danych (Bean Validation) → 400 z mapą pole → komunikat. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), String.valueOf(error.getDefaultMessage()));
        }
        ProblemDetail problem = ex.getBody();
        problem.setTitle("Niepoprawne dane");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }
}
