package s04_web_rest.solutions.rest05_crud_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa rozwiązań lekcji Rest05CrudApi — potrzebna testom rozwiązań ({@code @WebMvcTest} szuka
 * {@code @SpringBootConfiguration} w górę drzewa pakietów). Skanuje tylko pakiet solutions.rest05_crud_api.
 */
@SpringBootApplication
public class Rest05SolutionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(Rest05SolutionsApplication.class, args);
    }
}
