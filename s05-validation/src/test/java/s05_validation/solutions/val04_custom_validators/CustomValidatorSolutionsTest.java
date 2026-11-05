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
import s05_validation.solutions.val04_custom_validators.CustomValidatorSolutions.Address;
import s05_validation.solutions.val04_custom_validators.CustomValidatorSolutions.Registration;
import s05_validation.solutions.val04_custom_validators.CustomValidatorSolutions.Tagged;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy rozwiązań wzorcowych lekcji Val04CustomValidators. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
class CustomValidatorSolutionsTest {

    static ValidatorFactory factory;
    static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    @Test
    @DisplayName("Wzorzec 1: PostalCodeValidator — 00-950 tak; 00950, 0-950 i 00-95a nie; null tak")
    void exercise1() {
        CustomValidatorSolutions.PostalCodeValidator postal = new CustomValidatorSolutions.PostalCodeValidator();

        assertThat(postal.isValid("00-950", null)).isTrue();
        assertThat(postal.isValid(null, null)).isTrue();
        for (String bad : List.of("00950", "0-950", "00-95a", "00-9500", " 00-950", "")) {
            assertThat(postal.isValid(bad, null)).as(bad).isFalse();
        }
        assertThat(validator.validate(new Address("00-950"))).isEmpty();
        assertThat(validator.validate(new Address("zły"))).hasSize(1);
    }

    @Test
    @DisplayName("Wzorzec 2: PasswordsMatchValidator — różne hasła dają jedno naruszenie na polu repeat")
    void exercise2() {
        assertThat(validator.validate(new Registration("tajne", "tajne"))).isEmpty();
        assertThat(validator.validate(new Registration(null, "x"))).isEmpty();
        assertThat(validator.validate(new Registration("x", null))).isEmpty();

        Set<ConstraintViolation<Registration>> violations = validator.validate(new Registration("tajne", "inne"));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("repeat");
    }

    @Test
    @DisplayName("Wzorzec 3: NoDuplicatesValidator — duplikat bez względu na wielkość liter; null i elementy null pomijane")
    void exercise3() {
        CustomValidatorSolutions.NoDuplicatesValidator noDuplicates = new CustomValidatorSolutions.NoDuplicatesValidator();

        assertThat(noDuplicates.isValid(List.of("kawa", "herbata"), null)).isTrue();
        assertThat(noDuplicates.isValid(List.of(), null)).isTrue();
        assertThat(noDuplicates.isValid(null, null)).isTrue();
        assertThat(noDuplicates.isValid(List.of("Kawa", "herbata", "kawa"), null)).isFalse();
        assertThat(noDuplicates.isValid(new ArrayList<>(Arrays.asList("a", null, null)), null))
                .as("dwa null to nie duplikat tekstu").isTrue();
        assertThat(validator.validate(new Tagged(List.of("x", "X")))).hasSize(1);
    }
}
