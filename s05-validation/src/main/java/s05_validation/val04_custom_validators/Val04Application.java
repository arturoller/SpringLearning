package s05_validation.val04_custom_validators;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Val04CustomValidators. Lekcja nie ma kontrolerów — kontekst Springa jest potrzebny tylko po to,
 * żeby pokazać, że walidator może mieć wstrzykniętą zależność ({@link ProductCatalog}).
 */
@SpringBootApplication
public class Val04Application {

    public static void main(String[] args) {
        SpringApplication.run(Val04Application.class, args);
    }
}
