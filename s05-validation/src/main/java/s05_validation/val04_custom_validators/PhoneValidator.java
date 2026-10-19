package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * PhoneValidator = SZKIELET do ćwiczenia 1 lekcji Val04CustomValidators. Rozwiązanie wzorcowe: solutions.val04_custom_validators.
 */
public class PhoneValidator implements ConstraintValidator<Phone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // TODO: null → true; poprawne: "600100200", "600 100 200", "+48 600 100 200"; błędne: "12345", "abc", "+49 600 100 200"
        throw new UnsupportedOperationException("TODO");
    }
}
