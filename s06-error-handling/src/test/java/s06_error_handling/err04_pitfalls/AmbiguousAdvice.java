package s06_error_handling.err04_pitfalls;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * ZŁY advice (pułapka z sekcji 4 lekcji Err04ProblemDetail): dziedziczy po ResponseEntityExceptionHandler, który już obsługuje
 * {@code MethodArgumentNotValidException}, a mimo to dopisuje własny {@code @ExceptionHandler} dla tego samego wyjątku.
 */
@RestControllerAdvice
public class AmbiguousAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onInvalid(MethodArgumentNotValidException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dane są niepoprawne");
    }
}
