package s05_validation.solutions.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Val04CustomValidators (test: Val04CustomValidatorsSolutionsTest).
 * Adnotacje i walidatory są zagnieżdżone w jednej klasie — walidatory muszą być PUBLICZNE i mieć publiczny konstruktor
 * bezparametrowy, inaczej Hibernate Validator ich nie utworzy.
 */
public final class Val04CustomValidatorsSolutions {

    private Val04CustomValidatorsSolutions() {
    }

    // --- rozwiązanie 1: kod pocztowy -----------------------------------------------------------------

    @Constraint(validatedBy = PostalCodeValidator.class)
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface PostalCode {
        String message() default "Kod pocztowy ma postać 00-000";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class PostalCodeValidator implements ConstraintValidator<PostalCode, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || value.matches("\\d{2}-\\d{3}");   // matches pasuje do całego tekstu
        }
    }

    public record Address(@PostalCode String postalCode) {
    }

    // --- rozwiązanie 2: najwyżej N słów ----------------------------------------------------------------

    @Constraint(validatedBy = MaxWordsValidator.class)
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface MaxWords {
        int value();

        String message() default "Za dużo słów";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class MaxWordsValidator implements ConstraintValidator<MaxWords, String> {
        private static final Pattern WHITESPACE = Pattern.compile("\\s+");
        private int max;

        @Override
        public void initialize(MaxWords annotation) {
            this.max = annotation.value();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return true;   // 0 słów
            }
            return WHITESPACE.split(value.trim()).length <= max;
        }
    }

    public record Comment(@MaxWords(3) String text) {
    }

    // --- rozwiązanie 3: zgodność haseł (reguła klasowa) ---------------------------------------------

    @Constraint(validatedBy = PasswordsMatchValidator.class)
    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface PasswordsMatch {
        String message() default "Hasła muszą być takie same";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, Registration> {
        @Override
        public boolean isValid(Registration value, ConstraintValidatorContext context) {
            if (value == null || value.password() == null || value.confirmPassword() == null) {
                return true;
            }
            if (Objects.equals(value.password(), value.confirmPassword())) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
            return false;
        }
    }

    @PasswordsMatch
    public record Registration(String password, String confirmPassword) {
    }
}
