package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Sku} = własne ograniczenie: kod produktu w formacie {@code ELE-001} (trzy wielkie litery, myślnik, trzy cyfry).
 * Każde ograniczenie Bean Validation musi mieć trzy atrybuty: {@code message} (komunikat), {@code groups} (grupy,
 * Val03) i {@code payload} (dodatkowe dane dla narzędzi) — bez nich walidator odrzuci adnotację przy starcie.
 * Logikę sprawdzania zawiera {@link SkuValidator}, wskazany w {@code @Constraint(validatedBy = ...)}.
 */
@Documented
@Constraint(validatedBy = SkuValidator.class)   // constraint = ograniczenie; validated by = sprawdzane przez
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)             // retention = zachowanie adnotacji: RUNTIME — widoczna w działającym programie
public @interface Sku {

    String message() default "SKU ma format ELE-001 (trzy wielkie litery, myślnik, trzy cyfry)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
