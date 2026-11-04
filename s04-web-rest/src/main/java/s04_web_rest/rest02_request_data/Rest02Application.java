package s04_web_rest.rest02_request_data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest02RequestData. Testy lekcji ({@code @WebMvcTest}) znajdują ją same.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s04-web-rest spring-boot:run -Dspring-boot.run.main-class=s04_web_rest.rest02_request_data.Rest02Application}.
 * Gotowe zapytania: {@code s04-web-rest/http/rest02_request_data.http}.
 */
@SpringBootApplication
public class Rest02Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest02Application.class, args);
    }
}
