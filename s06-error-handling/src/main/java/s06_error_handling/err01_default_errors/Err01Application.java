package s06_error_handling.err01_default_errors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Err01DefaultErrors. Testy lekcji ({@code @SpringBootTest}, {@code @WebMvcTest}) znajdują ją same.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s06-error-handling spring-boot:run -Dspring-boot.run.main-class=s06_error_handling.err01_default_errors.Err01Application}.
 * Gotowe zapytania: {@code s06-error-handling/http/err01_default_errors.http}.
 */
@SpringBootApplication
public class Err01Application {

    public static void main(String[] args) {
        SpringApplication.run(Err01Application.class, args);
    }
}
