package s04_web_rest.solutions.rest03_responses;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa rozwiązań lekcji Rest03Responses — potrzebna testom rozwiązań ({@code @WebMvcTest} szuka
 * {@code @SpringBootConfiguration} w górę drzewa pakietów). Skanuje tylko pakiet solutions.rest03_responses.
 */
@SpringBootApplication
public class Rest03SolutionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(Rest03SolutionsApplication.class, args);
    }
}
