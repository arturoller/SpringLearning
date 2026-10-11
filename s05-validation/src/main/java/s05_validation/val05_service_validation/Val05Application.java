package s05_validation.val05_service_validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Val05ServiceValidation. Skanuje tylko pakiet s05_validation.val05_service_validation.
 * Uwaga: ShopProperties (sekcja 4) NIE jest tu zarejestrowane — zrobią to testy (ApplicationContextRunner),
 * żeby błędny start aplikacji nie psuł pozostałych testów lekcji.
 */
@SpringBootApplication
public class Val05Application {

    public static void main(String[] args) {
        SpringApplication.run(Val05Application.class, args);
    }
}
