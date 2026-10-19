package s05_validation.solutions.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Rozwiązanie wzorcowe ćwiczenia 1 lekcji Val04CustomValidators: adnotacja {@code @PhoneSolution} z walidatorem w środku. */
@Documented
@Constraint(validatedBy = PhoneSolution.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneSolution {

    String message() default "Numer telefonu: 9 cyfr, opcjonalnie z prefiksem +48 i spacjami";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Walidator: usuń spacje, odetnij prefiks +48, sprawdź czy zostało dokładnie 9 cyfr. */
    class Validator implements ConstraintValidator<PhoneSolution, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            String compact = value.replace(" ", "");
            if (compact.startsWith("+48")) {
                compact = compact.substring(3);
            }
            return compact.matches("[0-9]{9}");
        }
    }
}
