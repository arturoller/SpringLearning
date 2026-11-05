package s06_error_handling.err02_exception_handler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Err02ExceptionHandler. Testy lekcji ({@code @WebMvcTest}, {@code @SpringBootTest}) znajdują ją same.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s06-error-handling spring-boot:run -Dspring-boot.run.main-class=s06_error_handling.err02_exception_handler.Err02Application}.
 * Gotowe zapytania: {@code s06-error-handling/http/err02_exception_handler.http}.
 */
@SpringBootApplication
public class Err02Application {

    public static void main(String[] args) {
        SpringApplication.run(Err02Application.class, args);
    }
}
