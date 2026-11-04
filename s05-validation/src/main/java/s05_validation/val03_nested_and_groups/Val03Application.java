package s05_validation.val03_nested_and_groups;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Val03NestedAndGroups. Testy lekcji ({@code @WebMvcTest}, {@code @SpringBootTest}) znajdują ją same.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s05-validation spring-boot:run -Dspring-boot.run.main-class=s05_validation.val03_nested_and_groups.Val03Application}.
 * Gotowe zapytania: {@code s05-validation/http/val03_nested_and_groups.http}.
 */
@SpringBootApplication
public class Val03Application {

    public static void main(String[] args) {
        SpringApplication.run(Val03Application.class, args);
    }
}
