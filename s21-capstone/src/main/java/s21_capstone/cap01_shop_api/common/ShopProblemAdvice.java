package s21_capstone.cap01_shop_api.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
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
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * JEDNO miejsce, w którym błędy API sklepu zamieniają się w odpowiedzi {@code application/problem+json}
 * (ProblemDetail, RFC 9457 — s06_error_handling/Err04ProblemDetail).
 *
 * <ul>
 *   <li>klasa bazowa {@code ResponseEntityExceptionHandler} (obsługa wyjątków odpowiedzi) zna wyjątki Spring MVC:
 *       zły JSON, zła metoda, zły typ parametru, walidacja parametrów metody — każdy dostaje ProblemDetail;</li>
 *   <li>{@link ShopException} — nasze wyjątki domeny: status wybiera {@code switch} po zapieczętowanej hierarchii;</li>
 *   <li>nadpisany {@code handleMethodArgumentNotValid} — błędy {@code @Valid} na ciele żądania z mapą
 *       {@code errors} (pole → komunikat);</li>
 *   <li>{@code Exception} — siatka bezpieczeństwa: 500 bez szczegółów, szczegóły tylko w logu.</li>
 * </ul>
 */
@RestControllerAdvice
public class ShopProblemAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ShopProblemAdvice.class);

    /** Adres-identyfikator rodzaju problemu (domena .example jest zarezerwowana na przykłady). */
    static final String TYPE_BASE = "https://sklep.example/problemy/";

    @ExceptionHandler(ShopException.class)
    public ProblemDetail onShopException(ShopException e) {
        HttpStatus status = switch (e) {
            case NotFoundException notFound -> HttpStatus.NOT_FOUND;            // 404: nie ma zasobu
            case ConflictException conflict -> HttpStatus.CONFLICT;             // 409: kłóci się ze stanem danych
            case InvalidRequestException invalid -> HttpStatus.BAD_REQUEST;     // 400: zły parametr zapytania
        };
        log.info("Błąd klienta [{}]: {}", e.code(), e.getMessage());   // 4xx to nie awaria — bez stosu wywołań
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setTitle(status.getReasonPhrase());   // stały tytuł dla rodzaju problemu, opis w detail
        problem.setType(URI.create(TYPE_BASE + e.code().toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setProperty("code", e.code());
        e.details().forEach(problem::setProperty);
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new TreeMap<>();   // TreeMap — stała kolejność pól w odpowiedzi
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), String.valueOf(error.getDefaultMessage()));
        }
        ProblemDetail problem = ex.getBody();
        problem.setTitle("Niepoprawne dane");
        problem.setProperty("code", "VALIDATION");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /**
     * Błędy walidacji PARAMETRÓW metody ({@code @Max(50) int size} przy {@code @RequestParam}). Klasa bazowa daje 400
     * z ogólnym „Validation failure” — dopisujemy, KTÓRY parametr i dlaczego.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                errors.put(result.getMethodParameter().getParameterName(), String.valueOf(error.getDefaultMessage()));
            }
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
