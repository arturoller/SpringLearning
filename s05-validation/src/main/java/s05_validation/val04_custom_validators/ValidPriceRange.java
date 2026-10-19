package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** {@code @ValidPriceRange} = ograniczenie klasy: cena minimalna nie większa od maksymalnej (ćwiczenie 2; adnotacja gotowa). */
@Documented
@Constraint(validatedBy = PriceRangeValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPriceRange {

    String message() default "Cena minimalna nie może być większa od maksymalnej";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
