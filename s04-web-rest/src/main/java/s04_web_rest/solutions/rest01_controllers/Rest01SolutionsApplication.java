package s04_web_rest.solutions.rest01_controllers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa rozwiązań lekcji Rest01Controllers. Potrzebna testom rozwiązań ({@code @WebMvcTest} szuka
 * {@code @SpringBootConfiguration} w górę drzewa pakietów) — skanuje tylko pakiet solutions.rest01_controllers.
 */
@SpringBootApplication
public class Rest01SolutionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(Rest01SolutionsApplication.class, args);
    }
}
