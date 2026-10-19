package s05_validation.solutions.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import s05_validation.val04_custom_validators.HasPriceRange;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Rozwiązanie wzorcowe ćwiczenia 2 lekcji Val04CustomValidators: ograniczenie klasy {@code @PriceRangeSolution}. */
@Documented
@Constraint(validatedBy = PriceRangeSolution.Validator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PriceRangeSolution {

    String message() default "Cena minimalna nie może być większa od maksymalnej";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PriceRangeSolution, HasPriceRange> {

        @Override
        public boolean isValid(HasPriceRange value, ConstraintValidatorContext context) {
            if (value == null || value.minPrice() == null || value.maxPrice() == null) {
                return true;
            }
            if (value.minPrice().compareTo(value.maxPrice()) <= 0) {   // compareTo, nie equals (BigDecimal: 1.0 != 1.00)
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("maxPrice")
                    .addConstraintViolation();
            return false;
        }
    }
}
