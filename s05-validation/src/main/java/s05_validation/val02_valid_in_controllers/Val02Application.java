package s05_validation.val02_valid_in_controllers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Val02ValidInControllers. Skanuje tylko pakiet s05_validation.val02_valid_in_controllers
 * (rozwiązania ćwiczeń leżą poza nim i mają własną klasę startową). Po uruchomieniu ▶ wyślij zapytania z pliku
 * s05-validation/http/val02_valid_in_controllers.http.
 */
@SpringBootApplication
public class Val02Application {

    public static void main(String[] args) {
        SpringApplication.run(Val02Application.class, args);
    }
}
