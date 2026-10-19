package s21_capstone.cap02_reservations.common;

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
 * Błędy API rezerwacji jako ProblemDetail (ten sam wzór co ShopProblemAdvice w Cap01ShopApi):
 * NotFound → 404, Conflict → 409, RuleViolation → 422, {@code @Valid} → 400 z mapą {@code errors}, reszta → 500.
 *
 * <p>Własne pola ({@code setProperty}) NIE mogą nazywać się jak pola standardowe (type, title, status, detail,
 * instance): Jackson wypisałby wtedy dwa klucze o tej samej nazwie, np. {@code "status":409,...,"status":"ANULOWANA"}
 * (tak było w pierwszej wersji tego projektu — wyszło przy sprawdzaniu pliku .http). Stąd {@code reservationStatus}.</p>
 */
@RestControllerAdvice
public class BookingProblemAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BookingProblemAdvice.class);

    @ExceptionHandler(BookingException.class)
    public ProblemDetail onBookingException(BookingException e) {
        HttpStatus status = switch (e) {
            case NotFoundException notFound -> HttpStatus.NOT_FOUND;
            case ConflictException conflict -> HttpStatus.CONFLICT;
            case RuleViolationException violation -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
        log.info("Błąd klienta [{}]: {}", e.code(), e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("https://serwis.example/problemy/"
                + e.code().toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setProperty("code", e.code());
        e.details().forEach(problem::setProperty);
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), String.valueOf(error.getDefaultMessage()));
        }
        ProblemDetail problem = ex.getBody();
        problem.setTitle("Niepoprawne dane");
        problem.setProperty("code", "VALIDATION");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail onUnexpected(Exception e) {
        log.error("[BLAD-WEWNETRZNY] Nieobsłużony wyjątek", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Wystąpił błąd wewnętrzny. Spróbuj ponownie później.");
        problem.setTitle("Błąd wewnętrzny");
        return problem;
    }
}
