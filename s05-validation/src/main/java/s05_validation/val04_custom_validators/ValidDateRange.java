package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Ograniczenie na CAŁEJ KLASIE (class-level): promocja nie może kończyć się przed początkiem. Zob. {@link Promotion}. */
@Documented
@Constraint(validatedBy = ValidDateRangeValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})   // TYPE = klasa/rekord/interfejs
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {

    String message() default "{val04.promotion.range}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
