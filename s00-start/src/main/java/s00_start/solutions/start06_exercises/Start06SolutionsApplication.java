package s00_start.solutions.start06_exercises;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa ROZWIĄZAŃ lekcji Start06Exercises. Potrzebna, bo test @WebMvcTest rozwiązania szuka konfiguracji
 * Springa w górę drzewa pakietów — a klasa startowa lekcji leży w innej gałęzi (s00_start.start06_exercises).
 * Skanuje tylko s00_start.solutions.start06_exercises, więc nie widzi szkieletu DiscountController.
 */
@SpringBootApplication
public class Start06SolutionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(Start06SolutionsApplication.class, args);
    }
}
