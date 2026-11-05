package s05_validation.val04_custom_validators;

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

/**
 * SZKIELET ĆWICZEŃ lekcji Val04CustomValidators — uzupełniasz trzy walidatory (opis zadań: lekcja, sekcja ĆWICZENIA;
 * testy: Val04CustomValidatorsExercisesTest). Adnotacje i rekordy są gotowe; w walidatorach jest tylko
 * {@code throw new UnsupportedOperationException("TODO")}. Rozwiązania wzorcowe:
 * {@code s05_validation.solutions.val04_custom_validators.CustomValidatorSolutions}.
 */
public final class CustomValidatorExercises {

    private CustomValidatorExercises() {
    }

    // ---------------------------------------------------------------- ćwiczenie 1: @PostalCode

    @Documented
    @Constraint(validatedBy = PostalCodeValidator.class)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PostalCode {
        String message() default "Kod pocztowy ma postać 00-950";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    /** Ćwiczenie 1: polski kod pocztowy = dwie cyfry, myślnik, trzy cyfry. null jest poprawny. */
    public static class PostalCodeValidator implements ConstraintValidator<PostalCode, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    public record Address(@PostalCode String postalCode) {
    }

    // ---------------------------------------------------------------- ćwiczenie 2: @PasswordsMatch

    @Documented
    @Constraint(validatedBy = PasswordsMatchValidator.class)
    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PasswordsMatch {
        String message() default "Hasła nie są takie same";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    /**
     * Ćwiczenie 2: walidator klasowy. Gdy hasło i powtórzenie są różne, zgłoś naruszenie przypięte do pola
     * {@code repeat} (i wyłącz domyślne naruszenie klasowe). Gdy któreś jest null, zwróć true.
     */
    public static class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, Registration> {
        @Override
        public boolean isValid(Registration registration, ConstraintValidatorContext context) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    @PasswordsMatch
    public record Registration(String password, String repeat) {
    }

    // ---------------------------------------------------------------- ćwiczenie 3: @NoDuplicates

    @Documented
    @Constraint(validatedBy = NoDuplicatesValidator.class)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NoDuplicates {
        String message() default "Tagi nie mogą się powtarzać";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    /**
     * Ćwiczenie 3: kolekcja tekstów bez powtórzeń, nie rozróżniamy wielkości liter ("Kawa" i "kawa" to duplikat).
     * Kolekcja null jest poprawna, elementy null pomijamy.
     */
    public static class NoDuplicatesValidator implements ConstraintValidator<NoDuplicates, Collection<String>> {
        @Override
        public boolean isValid(Collection<String> values, ConstraintValidatorContext context) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    public record Tagged(@NoDuplicates java.util.List<String> tags) {
    }
}
