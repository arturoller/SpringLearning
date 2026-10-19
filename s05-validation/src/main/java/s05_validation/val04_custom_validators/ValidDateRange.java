package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @ValidDateRange} = ograniczenie na poziomie KLASY: data „od” nie może być późniejsza niż „do”.
 * Reguła dotyczy dwóch pól naraz, więc nie da się jej zapisać na żadnym z nich osobno.
 * Atrybut {@code reportOnField}: {@code true} — błąd przypięty do pola {@code to}; {@code false} — błąd całego obiektu.
 */
@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})   // type = klasa/rekord/interfejs
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {

    String message() default "Data początkowa nie może być późniejsza niż końcowa";

    boolean reportOnField() default true;   // własny atrybut adnotacji: walidator odczyta go w initialize()

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
