package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Własne ograniczenie: kod produktu (SKU = Stock Keeping Unit, "jednostka magazynowa") w postaci {@code ABC-1234} —
 * trzy duże litery, myślnik, cztery cyfry. Logika siedzi w {@link SkuValidator}.
 * <p>
 * Trzy atrybuty są OBOWIĄZKOWE dla każdego ograniczenia (spec. Jakarta Bean Validation): {@code message}, {@code groups},
 * {@code payload}. Bez nich walidator zgłosi {@code ConstraintDefinitionException} przy pierwszym użyciu.
 */
@Documented                                                         // documented = trafia do Javadoc użytkowników
@Constraint(validatedBy = SkuValidator.class)                       // constraint = ograniczenie; validatedBy = kto sprawdza
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE}) // target = gdzie wolno
@Retention(RetentionPolicy.RUNTIME)                                 // retention = jak długo żyje; RUNTIME, żeby walidator ją widział
public @interface Sku {

    /** Klucz w ValidationMessages.properties (szablon komunikatu). */
    String message() default "{val04.sku.invalid}";

    /** Grupy walidacji (Val03NestedAndGroups). */
    Class<?>[] groups() default {};

    /** Dodatkowe dane dla klienta walidatora (rzadko używane). */
    Class<? extends Payload>[] payload() default {};
}
