package s04_web_rest.solutions.rest06_cors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa rozwiązań lekcji Rest06Cors — potrzebna testom rozwiązań ({@code @WebMvcTest} szuka
 * {@code @SpringBootConfiguration} w górę drzewa pakietów). Skanuje tylko pakiet solutions.rest06_cors.
 */
@SpringBootApplication
public class Rest06SolutionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(Rest06SolutionsApplication.class, args);
    }
}
