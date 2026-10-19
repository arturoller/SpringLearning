package s05_validation.solutions.val03_nested_and_groups;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s05_validation.val03_nested_and_groups.OnCreate;
import s05_validation.val03_nested_and_groups.OnUpdate;
import s05_validation.val03_nested_and_groups.OrderLine;
import s05_validation.val03_nested_and_groups.OrderRequest;
import s05_validation.val03_nested_and_groups.ProductDto;
import s05_validation.val03_nested_and_groups.ShippingAddress;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy rozwiązań wzorcowych ćwiczeń Val03NestedAndGroups — działają domyślnie (dowód, że ćwiczenia da się rozwiązać). */
@Tag("wzorzec")
class Val03NestedAndGroupsSolutionsTest {

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
    @DisplayName("Rozwiązanie 1: posortowane ścieżki naruszeń (z opcjonalnymi grupami)")
    void solution1() {
        OrderRequest order = new OrderRequest("zle", new ShippingAddress("", "Kraków", "80-001"),
                List.of(new OrderLine("ELE-001", 0)), null);
        assertThat(Val03NestedAndGroupsSolutions.paths(validator, order))
                .containsExactly("address.street", "customerEmail", "lines[0].quantity");

        ProductDto product = new ProductDto(5L, "Kawa", BigDecimal.ONE, null);
        assertThat(Val03NestedAndGroupsSolutions.paths(validator, product)).isEmpty();
        assertThat(Val03NestedAndGroupsSolutions.paths(validator, product, OnCreate.class)).containsExactly("id");
    }

    @Test
    @DisplayName("Rozwiązanie 2: kaskada w ReturnRequestSolution")
    void solution2() {
        ShippingAddress address = new ShippingAddress("Długa 5", "Gdańsk", "80-001");
        OrderLine line = new OrderLine("ELE-001", 1);

        assertThat(validator.validate(new ReturnRequestSolution("ZAM-001", address, List.of(line)))).isEmpty();
        assertThat(Val03NestedAndGroupsSolutions.paths(validator, new ReturnRequestSolution("ZAM-001", null, List.of(line))))
                .as("brak adresu").containsExactly("address");
        assertThat(Val03NestedAndGroupsSolutions.paths(validator,
                new ReturnRequestSolution("ZAM-001", new ShippingAddress("", "Gdańsk", "80-001"), List.of(line))))
                .as("zły adres w środku").containsExactly("address.street");
        assertThat(Val03NestedAndGroupsSolutions.paths(validator, new ReturnRequestSolution("ZAM-001", address, List.of())))
                .as("pusta lista").containsExactly("lines");
        assertThat(Val03NestedAndGroupsSolutions.paths(validator,
                new ReturnRequestSolution("ZAM-001", address, List.of(line, new OrderLine("x", 1)))))
                .as("zła pozycja w środku").containsExactly("lines[1].sku");
    }

    @Test
    @DisplayName("Rozwiązanie 3: grupy OnCreate i OnUpdate na EmployeeDtoSolution")
    void solution3() {
        BigDecimal pay = new BigDecimal("9800");

        assertThat(validator.validate(new EmployeeDtoSolution(null, "Piotr Kowalski", null), OnCreate.class)).isEmpty();
        assertThat(Val03NestedAndGroupsSolutions.paths(validator, new EmployeeDtoSolution(1L, "Piotr Kowalski", pay), OnCreate.class))
                .containsExactly("id");
        assertThat(Val03NestedAndGroupsSolutions.paths(validator, new EmployeeDtoSolution(null, " ", pay), OnCreate.class))
                .containsExactly("name");
        assertThat(Val03NestedAndGroupsSolutions.paths(validator,
                new EmployeeDtoSolution(null, "Piotr Kowalski", BigDecimal.ZERO), OnCreate.class))
                .containsExactly("salary");

        assertThat(validator.validate(new EmployeeDtoSolution(1L, "Piotr Kowalski", pay), OnUpdate.class)).isEmpty();
        assertThat(Val03NestedAndGroupsSolutions.paths(validator, new EmployeeDtoSolution(null, "Piotr Kowalski", null), OnUpdate.class))
                .containsExactly("id", "salary");
    }
}
