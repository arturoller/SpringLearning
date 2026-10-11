package s05_validation.solutions.val02_valid_in_controllers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa ROZWIĄZAŃ lekcji Val02ValidInControllers. Potrzebna, bo test @WebMvcTest rozwiązania szuka konfiguracji
 * w górę drzewa pakietów, a klasa startowa lekcji leży w innej gałęzi. Skanuje tylko ten pakiet.
 */
@SpringBootApplication
public class Val02SolutionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(Val02SolutionsApplication.class, args);
    }
}
