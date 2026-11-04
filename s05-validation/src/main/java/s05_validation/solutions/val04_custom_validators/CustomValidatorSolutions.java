package s05_validation.solutions.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Val04CustomValidators. Te same adnotacje i rekordy co w
 * {@code CustomValidatorExercises}, ale z działającymi walidatorami.
 */
public final class CustomValidatorSolutions {

    private CustomValidatorSolutions() {
    }

    @Documented
    @Constraint(validatedBy = PostalCodeValidator.class)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PostalCode {
        String message() default "Kod pocztowy ma postać 00-950";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class PostalCodeValidator implements ConstraintValidator<PostalCode, String> {
        private static final Pattern POSTAL = Pattern.compile("[0-9]{2}-[0-9]{3}");

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || POSTAL.matcher(value).matches();
        }
    }

    public record Address(@PostalCode String postalCode) {
    }

    @Documented
    @Constraint(validatedBy = PasswordsMatchValidator.class)
    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PasswordsMatch {
        String message() default "Hasła nie są takie same";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, Registration> {
        @Override
        public boolean isValid(Registration registration, ConstraintValidatorContext context) {
            if (registration == null || registration.password() == null || registration.repeat() == null) {
                return true;
            }
            if (registration.password().equals(registration.repeat())) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("repeat")
                    .addConstraintViolation();
            return false;
        }
    }

    @PasswordsMatch
    public record Registration(String password, String repeat) {
    }

    @Documented
    @Constraint(validatedBy = NoDuplicatesValidator.class)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NoDuplicates {
        String message() default "Tagi nie mogą się powtarzać";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class NoDuplicatesValidator implements ConstraintValidator<NoDuplicates, Collection<String>> {
        @Override
        public boolean isValid(Collection<String> values, ConstraintValidatorContext context) {
            if (values == null) {
                return true;
            }
            Set<String> seen = new HashSet<>();
            for (String value : values) {
                // add() zwraca false, gdy element już był — to duplikat; ROOT = niezależnie od ustawień językowych komputera
                if (value != null && !seen.add(value.toLowerCase(Locale.ROOT))) {
                    return false;
                }
            }
            return true;
        }
    }

    public record Tagged(@NoDuplicates List<String> tags) {
    }
}
