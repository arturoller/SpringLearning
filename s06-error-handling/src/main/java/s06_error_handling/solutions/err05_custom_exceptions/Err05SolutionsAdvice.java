package s06_error_handling.solutions.err05_custom_exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import s06_error_handling.solutions.err05_custom_exceptions.Err05SolutionsController.ExerciseException;
import s06_error_handling.solutions.err05_custom_exceptions.Err05SolutionsController.ExerciseNotFoundException;
import s06_error_handling.solutions.err05_custom_exceptions.Err05SolutionsController.ExerciseRuleException;
import s06_error_handling.solutions.err05_custom_exceptions.Err05SolutionsController.InvoiceAlreadyPaidException;
import s06_error_handling.solutions.err05_custom_exceptions.Err05SolutionsController.WarehouseBusyException;

import java.net.URI;
import java.util.Locale;

/** Rozwiązanie wzorcowe advice z ćwiczeń 1-5 lekcji Err05CustomExceptions. */
@RestControllerAdvice
public class Err05SolutionsAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(Err05SolutionsAdvice.class);

    private static final String TYPE_BASE = "https://sklep.example/errors/";

    @ExceptionHandler(ExerciseNotFoundException.class)
    public ProblemDetail onNotFound(ExerciseNotFoundException exception) {
        return problem(exception, HttpStatus.NOT_FOUND, "Nie znaleziono zasobu");
    }

    @ExceptionHandler(ExerciseRuleException.class)
    public ProblemDetail onRule(ExerciseRuleException exception) {
        return problem(exception, HttpStatus.UNPROCESSABLE_CONTENT, "Naruszona reguła biznesowa");
    }

    @ExceptionHandler(InvoiceAlreadyPaidException.class)
    public ProblemDetail onInvoiceAlreadyPaid(InvoiceAlreadyPaidException exception) {
        return problem(exception, HttpStatus.CONFLICT, "Konflikt ze stanem sklepu");
    }

    @ExceptionHandler(WarehouseBusyException.class)
    public ResponseEntity<ProblemDetail> onWarehouseBusy(WarehouseBusyException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setProperty("code", "WAREHOUSE_BUSY");
        problem.setProperty("retryAfterSeconds", exception.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", String.valueOf(exception.getRetryAfterSeconds()))
                .body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail onAnything(Exception exception) {
        log.error("Nieoczekiwany błąd", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Wystąpił nieoczekiwany błąd");
        problem.setProperty("code", "UNEXPECTED");
        return problem;
    }

    private ProblemDetail problem(ExerciseException exception, HttpStatus status, String title) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(title);
        problem.setType(URI.create(TYPE_BASE + exception.getCode().toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setProperty("code", exception.getCode());
        return problem;
    }
}
