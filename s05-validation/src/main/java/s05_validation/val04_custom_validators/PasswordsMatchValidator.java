package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** SZKIELET ćwiczenia 3 z lekcji Val04CustomValidators (opis: Val04CustomValidators). */
public class PasswordsMatchValidator
        implements ConstraintValidator<PasswordsMatch, Val04CustomValidators.Exercise3Registration> {

    @Override
    public boolean isValid(Val04CustomValidators.Exercise3Registration value, ConstraintValidatorContext context) {
        // TODO: twoje rozwiązanie (błąd ma być przypięty do pola confirmPassword)
        throw new UnsupportedOperationException("TODO");
    }
}
