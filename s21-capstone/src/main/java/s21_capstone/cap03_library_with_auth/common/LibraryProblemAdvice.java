package s21_capstone.cap03_library_with_auth.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
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
 * Błędy API biblioteki jako ProblemDetail. Oprócz znanego wzoru (wyjątki domeny, {@code @Valid}, siatka 500) obsługuje
 * dwa wyjątki Spring Security, które wylatują z WNĘTRZA kontrolera i trafiają NAJPIERW do porad Spring MVC:
 * <ul>
 *   <li>{@link AccessDeniedException} (także AuthorizationDeniedException z {@code @PreAuthorize}) → 403;</li>
 *   <li>{@link AuthenticationException} (np. BadCredentialsException przy logowaniu) → 401.</li>
 * </ul>
 * Bez tych dwóch metod złapałaby je siatka {@code @ExceptionHandler(Exception.class)} i klient dostałby 500
 * (sprawdzone — lekcja Cap03LibraryWithAuth, sekcja 5).
 */
@RestControllerAdvice
public class LibraryProblemAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(LibraryProblemAdvice.class);

    @ExceptionHandler(LibraryException.class)
    public ProblemDetail onLibraryException(LibraryException e) {
        HttpStatus status = switch (e) {
            case NotFoundException notFound -> HttpStatus.NOT_FOUND;
            case ConflictException conflict -> HttpStatus.CONFLICT;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("https://biblioteka.example/problemy/"
                + e.code().toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setProperty("code", e.code());
        e.details().forEach(problem::setProperty);
        return problem;
    }

    /** {@code @PreAuthorize}/{@code @PostAuthorize} odmówiły (zalogowany, ale bez prawa do TYCH danych) → 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail onAccessDenied(AccessDeniedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Brak uprawnień do tego zasobu");
        problem.setTitle("Forbidden");
        problem.setProperty("code", "ACCESS_DENIED");
        return problem;
    }

    /** Nieudane logowanie (zły login lub hasło) → 401. Ten sam opis dla obu przypadków — nie zdradzamy, co było złe. */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail onAuthenticationFailed(AuthenticationException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Nieprawidłowy login lub hasło");
        problem.setTitle("Unauthorized");
        problem.setProperty("code", "BAD_CREDENTIALS");
        return problem;
    }

    /** Dwie osoby zmieniły ten sam wiersz naraz (@Version w Book) — przegrany dostaje 409, może spróbować ponownie. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail onConcurrentModification(OptimisticLockingFailureException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Dane zmieniły się w międzyczasie — odśwież i spróbuj ponownie");
        problem.setTitle("Conflict");
        problem.setProperty("code", "CONCURRENT_MODIFICATION");
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
