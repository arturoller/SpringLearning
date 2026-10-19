package s04_web_rest.solutions.rest04_json;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa rozwiązań lekcji Rest04Json — potrzebna testom rozwiązań ({@code @JsonTest} szuka
 * {@code @SpringBootConfiguration} w górę drzewa pakietów). Skanuje tylko pakiet solutions.rest04_json.
 */
@SpringBootApplication
public class Rest04SolutionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(Rest04SolutionsApplication.class, args);
    }
}
