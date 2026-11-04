package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To samo co {@link PhoneNumber}, ale BEZ {@code @ReportAsSingleViolation} (loud = "głośny"): każdy składnik zgłasza własne
 * naruszenie z własnym komunikatem. Służy do porównania w teście (sekcja 2).
 */
@NotBlank
@Pattern(regexp = "\\+?[0-9]{9,12}")
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumberLoud {

    String message() default "{val04.phone.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
