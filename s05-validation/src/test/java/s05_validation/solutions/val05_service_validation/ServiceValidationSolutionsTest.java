package s05_validation.solutions.val05_service_validation;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s05_validation.val05_service_validation.NewItem;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy rozwiązań wzorcowych lekcji Val05ServiceValidation. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
class ServiceValidationSolutionsTest {

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
    @DisplayName("Wzorzec 1: validateOrThrow — poprawny obiekt wraca, błędny kończy się ConstraintViolationException")
    void exercise1() {
        NewItem good = new NewItem("KAW-0001", 2);

        assertThat(ServiceValidationSolutions.validateOrThrow(validator, good)).isSameAs(good);
        assertThatThrownBy(() -> ServiceValidationSolutions.validateOrThrow(validator, new NewItem("", 0)))
                .isInstanceOf(ConstraintViolationException.class)
                .satisfies(e -> assertThat(((ConstraintViolationException) e).getConstraintViolations()).hasSize(2));
    }

    @Test
    @DisplayName("Wzorzec 2: describe — posortowane linie 'ścieżka (Adnotacja)'")
    void exercise2() {
        ConstraintViolationException exception = new ConstraintViolationException(validator.validate(new NewItem(" ", -1)));

        assertThat(ServiceValidationSolutions.describe(exception)).containsExactly("quantity (Positive)", "sku (NotBlank)");
    }

    @Test
    @DisplayName("Wzorzec 3: startsWith — poprawne ustawienia true, złe false")
    void exercise3() {
        assertThat(ServiceValidationSolutions.startsWith(Map.of(
                "val05.shop.name", "Sklep", "val05.shop.max-items-per-order", "10", "val05.shop.delivery.city", "Łódź"))).isTrue();
        assertThat(ServiceValidationSolutions.startsWith(Map.of(
                "val05.shop.name", "", "val05.shop.max-items-per-order", "10", "val05.shop.delivery.city", "Łódź"))).isFalse();
        assertThat(ServiceValidationSolutions.startsWith(Map.of())).isFalse();
    }
}
