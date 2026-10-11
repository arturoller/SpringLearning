package s05_validation.val04_custom_validators;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Val04CustomValidators. Skanuje tylko pakiet s05_validation.val04_custom_validators.
 * Po uruchomieniu ▶ wyślij zapytania z pliku s05-validation/http/val04_custom_validators.http.
 */
@SpringBootApplication
public class Val04Application {

    public static void main(String[] args) {
        SpringApplication.run(Val04Application.class, args);
    }
}
