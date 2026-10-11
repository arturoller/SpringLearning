package s05_validation.val01_bean_validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Val01BeanValidation. Lekcja nie ma kontrolerów — kontekst Springa jest potrzebny tylko
 * w jednym teście, który pokazuje gotowy bean {@code Validator} (walidator) z autokonfiguracji Boota.
 * Skanuje tylko pakiet s05_validation.val01_bean_validation.
 */
@SpringBootApplication
public class Val01Application {

    public static void main(String[] args) {
        SpringApplication.run(Val01Application.class, args);
    }
}
