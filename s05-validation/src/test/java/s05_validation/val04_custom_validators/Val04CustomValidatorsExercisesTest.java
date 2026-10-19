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

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Val04CustomValidators. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Val04CustomValidatorsExercisesTest {

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
    @DisplayName("Ćwiczenie 1: walidator @Phone")
    void exercise1() {
        for (String ok : new String[]{"600100200", "600 100 200", "+48 600 100 200", "+48600100200"}) {
            assertThat(validator.validate(new Contact("Jan Kowalski", ok))).as(ok).isEmpty();
        }
        assertThat(validator.validate(new Contact("Jan Kowalski", null))).as("null").isEmpty();
        for (String bad : new String[]{"12345", "abc", "+49 600 100 200", "6001002001", ""}) {
            assertThat(validator.validate(new Contact("Jan Kowalski", bad))).as(bad).hasSize(1);
        }
    }

    @Test
    @DisplayName("Ćwiczenie 2: walidator klasy @ValidPriceRange z błędem przypiętym do maxPrice")
    void exercise2() {
        assertThat(validator.validate(new PriceFilter(new BigDecimal("10"), new BigDecimal("20")))).isEmpty();
        assertThat(validator.validate(new PriceFilter(new BigDecimal("10.0"), new BigDecimal("10.00")))).isEmpty();
        assertThat(validator.validate(new PriceFilter(null, new BigDecimal("20")))).isEmpty();
        assertThat(validator.validate(new PriceFilter(new BigDecimal("10"), null))).isEmpty();

        Set<ConstraintViolation<PriceFilter>> violations =
                validator.validate(new PriceFilter(new BigDecimal("30"), new BigDecimal("20")));
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("maxPrice");
    }

    @Test
    @DisplayName("Ćwiczenie 3: ograniczenie złożone @Quantity (1–99, jeden komunikat)")
    void exercise3() {
        assertThat(validator.validate(new Basket("ELE-001", 1))).isEmpty();
        assertThat(validator.validate(new Basket("ELE-001", 99))).isEmpty();

        for (int bad : new int[]{0, 100, -5}) {
            Set<ConstraintViolation<Basket>> violations = validator.validate(new Basket("ELE-001", bad));
            assertThat(violations).as("ilość " + bad).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Ilość od 1 do 99");
        }
    }
}
