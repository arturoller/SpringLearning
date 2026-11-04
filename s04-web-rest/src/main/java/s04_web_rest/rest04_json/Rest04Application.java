package s04_web_rest.rest04_json;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest04Json. Testy lekcji ({@code @WebMvcTest}, {@code @JsonTest}) znajdują ją same.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s04-web-rest spring-boot:run -Dspring-boot.run.main-class=s04_web_rest.rest04_json.Rest04Application}.
 * Gotowe zapytania: {@code s04-web-rest/http/rest04_json.http}.
 */
@SpringBootApplication
public class Rest04Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest04Application.class, args);
    }
}
