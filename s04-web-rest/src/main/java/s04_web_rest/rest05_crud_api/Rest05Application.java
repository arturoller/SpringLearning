package s04_web_rest.rest05_crud_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest05CrudApi. Testy lekcji ({@code @WebMvcTest}, {@code @SpringBootTest}) znajdują ją same.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s04-web-rest spring-boot:run -Dspring-boot.run.main-class=s04_web_rest.rest05_crud_api.Rest05Application}.
 * Gotowe zapytania: {@code s04-web-rest/http/rest05_crud_api.http}.
 */
@SpringBootApplication
public class Rest05Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest05Application.class, args);
    }
}
