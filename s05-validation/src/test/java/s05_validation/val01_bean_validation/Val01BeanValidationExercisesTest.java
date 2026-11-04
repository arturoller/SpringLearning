package s05_validation.val01_bean_validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Val01BeanValidation. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Val01BeanValidationExercisesTest {

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

    private static final ProductForm GOOD = new ProductForm("Kawa", new BigDecimal("24.90"), 5, null);
    private static final ProductForm BAD = new ProductForm("  ", new BigDecimal("0.001"), 0, null);

    @Test
    @DisplayName("Ćwiczenie 1: isValid — true dla poprawnego, false dla błędnego formularza")
    void exercise1() {
        assertThat(ValidationExercises.isValid(validator, GOOD)).isTrue();
        assertThat(ValidationExercises.isValid(validator, BAD)).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 2: invalidFields — posortowane ścieżki bez powtórzeń")
    void exercise2() {
        assertThat(ValidationExercises.invalidFields(validator, GOOD)).isEmpty();
        assertThat(ValidationExercises.invalidFields(validator, BAD)).containsExactly("name", "price", "quantity");
    }

    @Test
    @DisplayName("Ćwiczenie 3: constraintsByField — pole → posortowane nazwy adnotacji")
    void exercise3() {
        Map<String, List<String>> result = ValidationExercises.constraintsByField(validator, BAD);

        assertThat(result).containsOnlyKeys("name", "price", "quantity");
        assertThat(result.get("name")).containsExactly("NotBlank", "Size");
        assertThat(result.get("price")).containsExactly("DecimalMin", "Digits");
        assertThat(result.get("quantity")).containsExactly("Min");
        assertThat(List.copyOf(result.keySet())).containsExactly("name", "price", "quantity");
    }

    @Test
    @DisplayName("Ćwiczenie 4: isPriceAllowed — sama cena, bez budowania formularza")
    void exercise4() {
        assertThat(ValidationExercises.isPriceAllowed(validator, new BigDecimal("24.90"))).isTrue();
        assertThat(ValidationExercises.isPriceAllowed(validator, new BigDecimal("0.00"))).isFalse();
        assertThat(ValidationExercises.isPriceAllowed(validator, new BigDecimal("1.234"))).isFalse();
        assertThat(ValidationExercises.isPriceAllowed(validator, null)).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 5: report — linie \"pole: Nazwa (wartość)\" posortowane")
    void exercise5() {
        assertThat(ValidationExercises.report(validator, new ProductForm(null, new BigDecimal("24.90"), 0, -5)))
                .containsExactly("discountPercent: PositiveOrZero (-5)", "name: NotBlank (null)", "quantity: Min (0)");
    }
}
