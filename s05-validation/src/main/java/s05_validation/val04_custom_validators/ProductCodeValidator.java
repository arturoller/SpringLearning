package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Walidator dla {@link ProductCode}: {@code ConstraintValidator<ProductCode, String>} = „sprawdzam adnotację ProductCode
 * na wartościach typu String”. Jedna instancja jest współdzielona przez wiele wątków — nie trzymaj w niej zmiennego stanu.
 */
public class ProductCodeValidator implements ConstraintValidator<ProductCode, String> {

    private Pattern pattern;

    /** Wołane raz, przed użyciem: odczytujemy atrybuty adnotacji (tu: liczbę cyfr). */
    @Override
    public void initialize(ProductCode annotation) {
        this.pattern = Pattern.compile("[A-Z]{3}-\\d{" + annotation.digits() + "}");
    }

    /** Wołane przy każdej walidacji. Konwencja: null jest poprawny — obowiązkowość to zadanie @NotNull. */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return pattern.matcher(value).matches();
    }
}
