package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** {@code @Phone} = polski numer telefonu — adnotacja do ćwiczenia 1 lekcji Val04CustomValidators (gotowa; walidator piszesz Ty). */
@Documented
@Constraint(validatedBy = PhoneValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Phone {

    String message() default "Numer telefonu: 9 cyfr, opcjonalnie z prefiksem +48 i spacjami";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
