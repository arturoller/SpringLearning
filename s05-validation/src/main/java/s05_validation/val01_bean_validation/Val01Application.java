package s05_validation.val01_bean_validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Val01BeanValidation. Lekcja nie ma kontrolerów — aplikacja służy tylko do pokazania, że Boot
 * sam tworzy bean (ziarno) {@code jakarta.validation.Validator} (test: Val01SpringValidatorTest).
 * <p>
 * Uruchomienie: ▶ obok {@code main}.
 */
@SpringBootApplication
public class Val01Application {

    public static void main(String[] args) {
        SpringApplication.run(Val01Application.class, args);
    }
}
