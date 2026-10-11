package s05_validation.val05_service_validation;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * SZKIELET ćwiczenia 2 z lekcji Val05ServiceValidation (opis: Val05ServiceValidation).
 * Klasa ma już {@code @Validated}. Test: Val05ServiceValidationExercisesTest.
 */
@Service
@Validated
public class Exercise2Service {

    /**
     * Kod produktu: pierwsze trzy litery nazwy (wielkie) + "-" + cztery cyfry z ceny. Dla nazwy krótszej niż 3 litery
     * wynik nie pasuje do wzorca ABC-1234 — i wtedy ma być zgłoszone naruszenie WYNIKU.
     */
    // TODO: ćwiczenie 2 — ograniczenie na wyniku (wzorzec [A-Z]{3}-\d{4}) i na parametrze (obowiązkowy, z kaskadą)
    public String codeFor(ProductCommand command) {
        String name = command.name().toUpperCase();
        String prefix = name.substring(0, Math.min(3, name.length()));
        return prefix + "-" + String.format("%04d", command.priceGrosze() % 10_000);
    }
}
