package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s05_validation.val04_custom_validators.Val04CustomValidators.Exercise1Address;
import s05_validation.val04_custom_validators.Val04CustomValidators.Exercise2Comment;
import s05_validation.val04_custom_validators.Val04CustomValidators.Exercise3Registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Val04CustomValidators. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Val04CustomValidatorsExercisesTest {

    static ValidatorFactory factory;
    static Validator validator;

    @BeforeAll
    static void create() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    static List<String> paths(Set<? extends ConstraintViolation<?>> violations) {
        List<String> result = new ArrayList<>();
        violations.forEach(v -> result.add(v.getPropertyPath().toString()));
        result.sort(String::compareTo);
        return result;
    }

    @Test
    @DisplayName("Ćwiczenie 1: kod pocztowy 00-000, null poprawny")
    void exercise1() {
        assertThat(validator.validate(new Exercise1Address("00-001"))).isEmpty();
        assertThat(validator.validate(new Exercise1Address(null))).isEmpty();
        assertThat(paths(validator.validate(new Exercise1Address("0001")))).containsExactly("postalCode");
        assertThat(paths(validator.validate(new Exercise1Address("00-0011")))).containsExactly("postalCode");
        assertThat(paths(validator.validate(new Exercise1Address("ab-cde")))).containsExactly("postalCode");
    }

    @Test
    @DisplayName("Ćwiczenie 2: najwyżej 3 słowa (atrybut value z adnotacji)")
    void exercise2() {
        assertThat(validator.validate(new Exercise2Comment("jedno dwa trzy"))).isEmpty();
        assertThat(validator.validate(new Exercise2Comment("  szybka   dostawa  "))).isEmpty();
        assertThat(validator.validate(new Exercise2Comment("   "))).isEmpty();
        assertThat(validator.validate(new Exercise2Comment(null))).isEmpty();
        assertThat(paths(validator.validate(new Exercise2Comment("a b c d")))).containsExactly("text");
    }

    @Test
    @DisplayName("Ćwiczenie 3: hasła muszą być równe, błąd przypięty do confirmPassword (jedno naruszenie)")
    void exercise3() {
        assertThat(validator.validate(new Exercise3Registration("tajne123", "tajne123"))).isEmpty();
        assertThat(validator.validate(new Exercise3Registration(null, "x"))).isEmpty();
        assertThat(validator.validate(new Exercise3Registration("x", null))).isEmpty();

        Set<ConstraintViolation<Exercise3Registration>> violations = validator.validate(new Exercise3Registration("a", "b"));
        assertThat(violations).hasSize(1);
        ConstraintViolation<Exercise3Registration> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("confirmPassword");
        assertThat(violation.getMessage()).isEqualTo("Hasła muszą być takie same");
    }
}
