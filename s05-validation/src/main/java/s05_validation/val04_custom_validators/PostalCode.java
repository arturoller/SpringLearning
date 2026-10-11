package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Ćwiczenie 1 (Val04CustomValidators): polski kod pocztowy "00-000". Adnotacja jest gotowa — brakuje walidatora. */
@Documented
@Constraint(validatedBy = PostalCodeValidator.class)
@Target({FIELD, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface PostalCode {

    String message() default "Kod pocztowy ma postać 00-000";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
