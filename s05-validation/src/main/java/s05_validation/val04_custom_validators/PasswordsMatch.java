package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Ćwiczenie 3 (Val04CustomValidators): hasło i jego powtórzenie muszą być takie same. Adnotacja gotowa — brakuje walidatora. */
@Documented
@Constraint(validatedBy = PasswordsMatchValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface PasswordsMatch {

    String message() default "Hasła muszą być takie same";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
