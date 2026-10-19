package s05_validation.solutions.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Rozwiązanie wzorcowe ćwiczenia 3 lekcji Val04CustomValidators: ograniczenie złożone z @Min i @Max. */
@Min(1)
@Max(99)
@ReportAsSingleViolation   // jedno naruszenie z komunikatem tej adnotacji
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface QuantitySolution {

    String message() default "Ilość od 1 do 99";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
