package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Quantity} = liczba sztuk w koszyku — SZKIELET do ćwiczenia 3 lekcji Val04CustomValidators.
 * TODO: złóż ograniczenie z gotowych: liczba od 1 do 99 (adnotacje @Min i @Max nad tą adnotacją), jedno naruszenie
 * z komunikatem {@code message()} (czyli "Ilość od 1 do 99"). Rozwiązanie wzorcowe: solutions.val04_custom_validators.
 */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Quantity {

    String message() default "Ilość od 1 do 99";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
