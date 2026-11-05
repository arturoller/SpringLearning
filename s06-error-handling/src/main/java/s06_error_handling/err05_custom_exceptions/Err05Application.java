package s06_error_handling.err05_custom_exceptions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Err05CustomExceptions. Testy lekcji ({@code @WebMvcTest}, {@code @SpringBootTest}) znajdują ją same.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s06-error-handling spring-boot:run -Dspring-boot.run.main-class=s06_error_handling.err05_custom_exceptions.Err05Application}.
 * Gotowe zapytania: {@code s06-error-handling/http/err05_custom_exceptions.http}.
 */
@SpringBootApplication
public class Err05Application {

    public static void main(String[] args) {
        SpringApplication.run(Err05Application.class, args);
    }
}
