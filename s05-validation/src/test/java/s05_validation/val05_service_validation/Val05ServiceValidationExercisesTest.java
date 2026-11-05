package s05_validation.val05_service_validation;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Val05ServiceValidation. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Val05ServiceValidationExercisesTest {

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
    @DisplayName("Ćwiczenie 1: validateOrThrow — poprawny obiekt wraca, błędny kończy się ConstraintViolationException")
    void exercise1() {
        NewItem good = new NewItem("KAW-0001", 2);

        assertThat(ServiceValidationExercises.validateOrThrow(validator, good)).isSameAs(good);
        assertThatThrownBy(() -> ServiceValidationExercises.validateOrThrow(validator, new NewItem("", 0)))
                .isInstanceOf(ConstraintViolationException.class)
                .satisfies(e -> assertThat(((ConstraintViolationException) e).getConstraintViolations()).hasSize(2));
    }

    @Test
    @DisplayName("Ćwiczenie 2: describe — posortowane linie 'ścieżka (Adnotacja)'")
    void exercise2() {
        ConstraintViolationException exception = new ConstraintViolationException(validator.validate(new NewItem(" ", -1)));

        assertThat(ServiceValidationExercises.describe(exception)).containsExactly("quantity (Positive)", "sku (NotBlank)");
    }

    @Test
    @DisplayName("Ćwiczenie 3: startsWith — poprawne ustawienia true, złe false")
    void exercise3() {
        assertThat(ServiceValidationExercises.startsWith(Map.of(
                "val05.shop.name", "Sklep", "val05.shop.max-items-per-order", "10", "val05.shop.delivery.city", "Łódź"))).isTrue();
        assertThat(ServiceValidationExercises.startsWith(Map.of(
                "val05.shop.name", "", "val05.shop.max-items-per-order", "10", "val05.shop.delivery.city", "Łódź"))).isFalse();
        assertThat(ServiceValidationExercises.startsWith(Map.of())).isFalse();
    }
}
