package s05_validation.solutions.val05_service_validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa ROZWIĄZAŃ lekcji Val05ServiceValidation. Test @SpringBootTest rozwiązania szuka konfiguracji w górę
 * drzewa pakietów, a klasa startowa lekcji leży w innej gałęzi. Skanuje tylko ten pakiet.
 */
@SpringBootApplication
public class Val05SolutionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(Val05SolutionsApplication.class, args);
    }
}
