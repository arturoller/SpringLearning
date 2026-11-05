package s05_validation.val02_valid_in_controllers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Val02ValidInControllers. Testy lekcji ({@code @WebMvcTest}, {@code @SpringBootTest}) znajdują ją same.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s05-validation spring-boot:run -Dspring-boot.run.main-class=s05_validation.val02_valid_in_controllers.Val02Application}.
 * Gotowe zapytania: {@code s05-validation/http/val02_valid_in_controllers.http}.
 */
@SpringBootApplication
public class Val02Application {

    public static void main(String[] args) {
        SpringApplication.run(Val02Application.class, args);
    }
}
