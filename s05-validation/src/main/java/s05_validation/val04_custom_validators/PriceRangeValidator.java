package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * PriceRangeValidator = SZKIELET do ćwiczenia 2 lekcji Val04CustomValidators. Rozwiązanie wzorcowe: solutions.val04_custom_validators.
 */
public class PriceRangeValidator implements ConstraintValidator<ValidPriceRange, HasPriceRange> {

    @Override
    public boolean isValid(HasPriceRange value, ConstraintValidatorContext context) {
        // TODO: obiekt albo któraś z cen null → true; min <= max → true; w przeciwnym razie false, ale
        //       błąd ma być przypięty do pola "maxPrice" (disableDefaultConstraintViolation + addPropertyNode)
        throw new UnsupportedOperationException("TODO");
    }
}
