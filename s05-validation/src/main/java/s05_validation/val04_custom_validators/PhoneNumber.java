package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ograniczenie ZŁOŻONE z istniejących (composed constraint): numer telefonu = niepusty + pasuje do wzorca. Nie ma własnego
 * walidatora ({@code validatedBy = {}}) — sprawdzają go adnotacje-składniki. {@code @ReportAsSingleViolation} (report as
 * single violation = zgłoś jako jedno naruszenie) zastępuje komunikaty składników JEDNYM komunikatem tej adnotacji.
 */
@NotBlank
@Pattern(regexp = "\\+?[0-9]{9,12}")
@ReportAsSingleViolation
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {

    String message() default "{val04.phone.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
