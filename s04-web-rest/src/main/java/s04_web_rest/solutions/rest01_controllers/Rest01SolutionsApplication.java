package s04_web_rest.solutions.rest01_controllers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa ROZWIĄZAŃ lekcji Rest01Controllers. Potrzebna, bo test @WebMvcTest rozwiązania szuka konfiguracji
 * w górę drzewa pakietów, a klasa startowa lekcji leży w innej gałęzi. Skanuje tylko ten pakiet — nie widzi szkieletu.
 */
@SpringBootApplication
public class Rest01SolutionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(Rest01SolutionsApplication.class, args);
    }
}
