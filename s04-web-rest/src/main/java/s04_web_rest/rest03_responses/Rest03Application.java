package s04_web_rest.rest03_responses;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest03Responses. Testy lekcji ({@code @WebMvcTest}) znajdują ją same.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s04-web-rest spring-boot:run -Dspring-boot.run.main-class=s04_web_rest.rest03_responses.Rest03Application}.
 * Gotowe zapytania: {@code s04-web-rest/http/rest03_responses.http}.
 */
@SpringBootApplication
public class Rest03Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest03Application.class, args);
    }
}
