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

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy rozwiązań wzorcowych ćwiczeń Val04CustomValidators — działają domyślnie (dowód, że ćwiczenia da się rozwiązać). */
@Tag("wzorzec")
class Val04CustomValidatorsSolutionsTest {

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
    @DisplayName("Rozwiązanie 1: walidator @PhoneSolution")
    void solution1() {
        for (String ok : new String[]{"600100200", "600 100 200", "+48 600 100 200", "+48600100200"}) {
            assertThat(validator.validate(new ContactSolution("Jan Kowalski", ok))).as(ok).isEmpty();
        }
        assertThat(validator.validate(new ContactSolution("Jan Kowalski", null))).as("null").isEmpty();
        for (String bad : new String[]{"12345", "abc", "+49 600 100 200", "6001002001", ""}) {
            assertThat(validator.validate(new ContactSolution("Jan Kowalski", bad))).as(bad).hasSize(1);
        }
    }

    @Test
    @DisplayName("Rozwiązanie 2: walidator klasy @PriceRangeSolution z błędem przypiętym do maxPrice")
    void solution2() {
        assertThat(validator.validate(new PriceFilterSolution(new BigDecimal("10"), new BigDecimal("20")))).isEmpty();
        assertThat(validator.validate(new PriceFilterSolution(new BigDecimal("10.0"), new BigDecimal("10.00")))).isEmpty();
        assertThat(validator.validate(new PriceFilterSolution(null, new BigDecimal("20")))).isEmpty();
        assertThat(validator.validate(new PriceFilterSolution(new BigDecimal("10"), null))).isEmpty();

        Set<ConstraintViolation<PriceFilterSolution>> violations =
                validator.validate(new PriceFilterSolution(new BigDecimal("30"), new BigDecimal("20")));
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("maxPrice");
    }

    @Test
    @DisplayName("Rozwiązanie 3: ograniczenie złożone @QuantitySolution (1–99, jeden komunikat)")
    void solution3() {
        assertThat(validator.validate(new BasketSolution("ELE-001", 1))).isEmpty();
        assertThat(validator.validate(new BasketSolution("ELE-001", 99))).isEmpty();

        for (int bad : new int[]{0, 100, -5}) {
            Set<ConstraintViolation<BasketSolution>> violations = validator.validate(new BasketSolution("ELE-001", bad));
            assertThat(violations).as("ilość " + bad).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Ilość od 1 do 99");
        }
    }
}
