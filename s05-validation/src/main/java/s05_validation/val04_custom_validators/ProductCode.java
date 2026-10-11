package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Własne ograniczenie {@code @ProductCode}: tekst w postaci trzech wielkich liter, myślnika i cyfr, np. "ABC-1234".
 * Liczbę cyfr ustawia atrybut {@code digits} (domyślnie 4). Samą kontrolę robi {@link ProductCodeValidator}.
 */
@Documented
@Constraint(validatedBy = ProductCodeValidator.class)   // constraint = ograniczenie; validatedBy = kto sprawdza
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})   // target = gdzie wolno użyć adnotacji
@Retention(RUNTIME)   // retention = jak długo żyje; RUNTIME = także w działającym programie (inaczej Validator jej nie zobaczy)
public @interface ProductCode {

    /** Szablon komunikatu; {digits} zostanie podstawione wartością atrybutu. */
    String message() default "Kod produktu ma postać ABC-{digits} cyfr";

    /** Wymagany przez specyfikację: grupy walidacji (lekcja Val03NestedAndGroups). */
    Class<?>[] groups() default {};

    /** Wymagany przez specyfikację: dodatkowe dane dla narzędzi (rzadko używane). */
    Class<? extends Payload>[] payload() default {};

    /** Liczba cyfr po myślniku. */
    int digits() default 4;
}
