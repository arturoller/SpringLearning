package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** {@code @KnownCategory} = kategoria musi istnieć w słowniku sklepu ({@link CategoryCatalog}). */
@Documented
@Constraint(validatedBy = KnownCategoryValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface KnownCategory {

    String message() default "Nieznana kategoria";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
