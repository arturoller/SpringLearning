package s06_error_handling.err03_controller_advice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Err03ControllerAdvice. Testy lekcji ({@code @WebMvcTest}, {@code @SpringBootTest}) znajdują ją same.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s06-error-handling spring-boot:run -Dspring-boot.run.main-class=s06_error_handling.err03_controller_advice.Err03Application}.
 * Gotowe zapytania: {@code s06-error-handling/http/err03_controller_advice.http}.
 */
@SpringBootApplication
public class Err03Application {

    public static void main(String[] args) {
        SpringApplication.run(Err03Application.class, args);
    }
}
