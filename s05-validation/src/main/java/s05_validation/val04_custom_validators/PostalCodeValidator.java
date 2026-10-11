package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** SZKIELET ćwiczenia 1 z lekcji Val04CustomValidators (opis: Val04CustomValidators). */
public class PostalCodeValidator implements ConstraintValidator<PostalCode, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // TODO: twoje rozwiązanie (pamiętaj o konwencji dla null)
        throw new UnsupportedOperationException("TODO");
    }
}
