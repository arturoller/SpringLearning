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

/** Ćwiczenie 2 (Val04CustomValidators): najwyżej {@code value} słów w tekście. Adnotacja gotowa — brakuje walidatora. */
@Documented
@Constraint(validatedBy = MaxWordsValidator.class)
@Target({FIELD, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface MaxWords {

    /** Dozwolona liczba słów. */
    int value();

    String message() default "Za dużo słów";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
