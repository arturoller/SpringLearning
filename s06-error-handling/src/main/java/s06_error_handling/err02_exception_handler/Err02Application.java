package s06_error_handling.err02_exception_handler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Err02ExceptionHandler. Skanuje tylko pakiet s06_error_handling.err02_exception_handler.
 * Gotowe zapytania: s06-error-handling/http/err02_exception_handler.http.
 */
@SpringBootApplication
public class Err02Application {

    public static void main(String[] args) {
        SpringApplication.run(Err02Application.class, args);
    }
}
