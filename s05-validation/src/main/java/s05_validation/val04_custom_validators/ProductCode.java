package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @ProductCode} = ograniczenie ZŁOŻONE (composed constraint): zamiast pisać walidator, składamy je z gotowych
 * adnotacji. {@code validatedBy = {}} mówi „nie ma własnej klasy walidatora”. Najprostszy sposób na własną nazwę
 * dla często używanego wzorca — o ile wystarczy to, co umieją wbudowane ograniczenia.
 */
@Pattern(regexp = "[A-Z]{3}-[0-9]{3}")   // pattern = wzorzec; adnotacja-składnik
@ReportAsSingleViolation                  // zgłaszaj JEDNO naruszenie z komunikatem tej adnotacji, nie składnika
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProductCode {

    String message() default "Kod produktu ma format ELE-001";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
