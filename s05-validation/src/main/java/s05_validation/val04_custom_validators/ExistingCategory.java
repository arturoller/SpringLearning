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

/**
 * Ograniczenie {@code @ExistingCategory}: kategoria musi istnieć w katalogu sklepu. Walidator potrzebuje beana Springa
 * (CategoryCatalog), więc zadziała tylko z Validatorem ze Springa — zob. sekcja 4 lekcji.
 */
@Documented
@Constraint(validatedBy = ExistingCategoryValidator.class)
@Target({FIELD, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface ExistingCategory {

    String message() default "Nie ma takiej kategorii";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
