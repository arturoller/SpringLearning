package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Walidator ograniczenia {@link Sku}: {@code ConstraintValidator<Sku, String>} = "sprawdzam adnotację Sku na typie String".
 * Walidator to zwykła klasa z publicznym konstruktorem bez argumentów — nie musi być beanem Springa.
 */
public class SkuValidator implements ConstraintValidator<Sku, String> {

    private static final Pattern SKU = Pattern.compile("[A-Z]{3}-[0-9]{4}");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;   // null = "brak wartości" — wymagalność to sprawa @NotNull (jak w ograniczeniach standardowych)
        }
        return SKU.matcher(value).matches();   // matches() = cały tekst musi pasować
    }
}
