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

/**
 * Testy ĆWICZEŃ lekcji Val01BeanValidation. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Spring nie jest potrzebny: używamy walidatora zbudowanego ręcznie.
 */
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

    @Test
    @DisplayName("Ćwiczenie 1: nazwy pól z błędami, posortowane")
    void exercise1() {
        assertThat(Val01BeanValidation.fieldsWithErrors(validator,
                new ProductForm("Kawa", "SPO-001", BigDecimal.ONE, 1, "Arabica"))).isEmpty();
        assertThat(Val01BeanValidation.fieldsWithErrors(validator,
                new ProductForm(" ", "abc", null, 5000, "ab")))
                .containsExactly("description", "name", "price", "sku", "stock");
    }

    @Test
    @DisplayName("Ćwiczenie 2: adnotacje na rekordzie EmployeeForm")
    void exercise2() {
        assertThat(validator.validate(new EmployeeForm("Anna Nowak", new BigDecimal("14500"), "anna@example.com")))
                .isEmpty();
        assertThat(validator.validate(new EmployeeForm(" ", new BigDecimal("14500"), "anna@example.com")))
                .as("imię z samych spacji").hasSize(1);
        assertThat(validator.validate(new EmployeeForm("Anna", null, "anna@example.com")))
                .as("brak pensji (null)").hasSize(1);
        assertThat(validator.validate(new EmployeeForm("Anna", BigDecimal.ZERO, "anna@example.com")))
                .as("pensja zero").hasSize(1);
        assertThat(validator.validate(new EmployeeForm("Anna", BigDecimal.TEN, null)))
                .as("brak e-maila (null)").hasSize(1);
        assertThat(validator.validate(new EmployeeForm("Anna", BigDecimal.TEN, "abc")))
                .as("zły format e-maila").hasSize(1);
    }

    @Test
    @DisplayName("Ćwiczenie 3: mapa pole → posortowane komunikaty")
    void exercise3() {
        assertThat(Val01BeanValidation.messagesByField(validator,
                new ProductForm("Kawa", "SPO-001", BigDecimal.ONE, 1, "Arabica"))).isEmpty();

        Map<String, List<String>> messages = Val01BeanValidation.messagesByField(validator,
                new ProductForm(" ", "abc", new BigDecimal("0.001"), 1, "ab"));

        assertThat(messages).containsExactly(
                Map.entry("description", List.of("Opis: od 3 do 20 znaków")),
                Map.entry("name", List.of("Nazwa produktu jest wymagana")),
                Map.entry("price", List.of("Cena musi wynosić co najmniej 0.01 zł")),
                Map.entry("sku", List.of("SKU ma format np. ELE-001")));
    }
}
