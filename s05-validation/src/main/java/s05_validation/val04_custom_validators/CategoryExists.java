package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Kategoria musi istnieć w {@link ProductCatalog}. Walidator ma wstrzykiwaną zależność — zob. {@link CategoryExistsValidator}. */
@Documented
@Constraint(validatedBy = CategoryExistsValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CategoryExists {

    String message() default "{val04.category.unknown}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
