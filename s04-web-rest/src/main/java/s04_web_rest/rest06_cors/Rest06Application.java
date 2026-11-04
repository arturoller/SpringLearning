package s04_web_rest.rest06_cors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest06Cors. Testy lekcji ({@code @WebMvcTest}) znajdują ją same.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s04-web-rest spring-boot:run -Dspring-boot.run.main-class=s04_web_rest.rest06_cors.Rest06Application}.
 * Gotowe zapytania: {@code s04-web-rest/http/rest06_cors.http}.
 */
@SpringBootApplication
public class Rest06Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest06Application.class, args);
    }
}
