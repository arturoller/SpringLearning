package s06_error_handling.solutions.err04_problem_detail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.TreeMap;

/** Rozwiązanie wzorcowe advice z ćwiczeń 1, 3, 4 i 5 lekcji Err04ProblemDetail. */
@RestControllerAdvice
public class Err04SolutionsAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(Err04SolutionsAdvice.class);

    @ExceptionHandler(Err04SolutionsController.CustomerNotFoundException.class)
    public ProblemDetail onCustomerNotFound(Err04SolutionsController.CustomerNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Klient nie istnieje");
        problem.setProperty("code", "CUSTOMER_NOT_FOUND");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException exception,
                                                                          HttpHeaders headers, HttpStatusCode status,
                                                                          WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Brakuje parametru " + exception.getParameterName());
        problem.setProperty("code", "MISSING_PARAMETER");
        problem.setProperty("parameter", exception.getParameterName());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new TreeMap<>();   // posortowane: odpowiedź zawsze w tej samej kolejności
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.merge(error.getField(), String.valueOf(error.getDefaultMessage()), (first, second) -> first + "; " + second);
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Dane są niepoprawne");
        problem.setTitle("Błąd walidacji");
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("fields", fields);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail onAnything(Exception exception) {
        log.error("Nieoczekiwany błąd", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Wystąpił nieoczekiwany błąd");
        problem.setProperty("code", "UNEXPECTED");
        return problem;
    }
}
