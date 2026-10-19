package s14_openapi.api02_demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Klasa pomocnicza testu lekcji Api02DocumentingApi (leży poza pakietem lekcji, żeby jej skanowanie jej nie znalazło —
 * test dokleja ją ręcznie przez {@code @Import}). Obsługa wyjątku z {@code @ResponseStatus} na metodzie obsługi.
 */
@RestControllerAdvice
public class StatusAdviceDemo {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail conflict(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
