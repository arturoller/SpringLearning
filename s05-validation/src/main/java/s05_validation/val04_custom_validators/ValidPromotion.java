package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Ograniczenie na poziomie KLASY (type = typ/klasa): sprawdza zależności między polami promocji — data „od” nie później
 * niż „do” i zniżka nie większa od ceny. Kontrola: {@link ValidPromotionValidator}.
 */
@Documented
@Constraint(validatedBy = ValidPromotionValidator.class)
@Target(TYPE)   // adnotacja stoi nad klasą/rekordem, nie nad polem
@Retention(RUNTIME)
public @interface ValidPromotion {

    String message() default "Niepoprawna promocja";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
