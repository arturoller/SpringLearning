package s05_validation.solutions.val04_custom_validators;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s05_validation.solutions.val04_custom_validators.Val04CustomValidatorsSolutions.Address;
import s05_validation.solutions.val04_custom_validators.Val04CustomValidatorsSolutions.Comment;
import s05_validation.solutions.val04_custom_validators.Val04CustomValidatorsSolutions.Registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Val04CustomValidatorsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Val04CustomValidatorsSolutionsTest {

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
    @DisplayName("Wzorzec 1: kod pocztowy 00-000, null poprawny")
    void exercise1() {
        assertThat(validator.validate(new Address("00-001"))).isEmpty();
        assertThat(validator.validate(new Address(null))).isEmpty();
        assertThat(paths(validator.validate(new Address("0001")))).containsExactly("postalCode");
        assertThat(paths(validator.validate(new Address("00-0011")))).containsExactly("postalCode");
        assertThat(paths(validator.validate(new Address("ab-cde")))).containsExactly("postalCode");
    }

    @Test
    @DisplayName("Wzorzec 2: najwyżej 3 słowa (atrybut value z adnotacji)")
    void exercise2() {
        assertThat(validator.validate(new Comment("jedno dwa trzy"))).isEmpty();
        assertThat(validator.validate(new Comment("  szybka   dostawa  "))).isEmpty();
        assertThat(validator.validate(new Comment("   "))).isEmpty();
        assertThat(validator.validate(new Comment(null))).isEmpty();
        assertThat(paths(validator.validate(new Comment("a b c d")))).containsExactly("text");
    }

    @Test
    @DisplayName("Wzorzec 3: hasła muszą być równe, błąd przypięty do confirmPassword (jedno naruszenie)")
    void exercise3() {
        assertThat(validator.validate(new Registration("tajne123", "tajne123"))).isEmpty();
        assertThat(validator.validate(new Registration(null, "x"))).isEmpty();
        assertThat(validator.validate(new Registration("x", null))).isEmpty();

        Set<ConstraintViolation<Registration>> violations = validator.validate(new Registration("a", "b"));
        assertThat(violations).hasSize(1);
        ConstraintViolation<Registration> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("confirmPassword");
        assertThat(violation.getMessage()).isEqualTo("Hasła muszą być takie same");
    }
}
