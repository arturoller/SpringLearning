package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * SkuValidator = logika adnotacji {@link Sku}. Interfejs {@code ConstraintValidator<A, T>}: A — adnotacja,
 * T — typ sprawdzanej wartości (tu String). Walidator jest tworzony raz na miejsce użycia adnotacji, więc nie może
 * trzymać zmiennego stanu zależnego od sprawdzanej wartości.
 */
public class SkuValidator implements ConstraintValidator<Sku, String> {

    private static final Pattern FORMAT = Pattern.compile("[A-Z]{3}-[0-9]{3}");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;   // null to sprawa @NotNull — walidator formatu go nie ocenia (Val01, sekcja 4)
        }
        return FORMAT.matcher(value).matches();
    }
}
