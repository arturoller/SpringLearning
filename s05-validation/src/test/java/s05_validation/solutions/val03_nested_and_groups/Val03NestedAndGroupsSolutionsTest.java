package s05_validation.solutions.val03_nested_and_groups;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s05_validation.solutions.val03_nested_and_groups.Val03NestedAndGroupsSolutions.Address;
import s05_validation.solutions.val03_nested_and_groups.Val03NestedAndGroupsSolutions.Shipment;
import s05_validation.solutions.val03_nested_and_groups.Val03NestedAndGroupsSolutions.Basket;
import s05_validation.solutions.val03_nested_and_groups.Val03NestedAndGroupsSolutions.Line;
import s05_validation.solutions.val03_nested_and_groups.Val03NestedAndGroupsSolutions.Customer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Val03NestedAndGroupsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Val03NestedAndGroupsSolutionsTest {

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
    @DisplayName("Wzorzec 1: przesyłka — odbiorca i obowiązkowy adres sprawdzany w głąb")
    void exercise1() {
        assertThat(validator.validate(new Shipment("Ala", new Address("Gdańsk")))).isEmpty();
        assertThat(paths(validator.validate(new Shipment(" ", new Address("Gdańsk")))))
                .containsExactly("receiver");
        assertThat(paths(validator.validate(new Shipment("Ala", null)))).containsExactly("address");
        assertThat(paths(validator.validate(new Shipment("Ala", new Address(""))))).containsExactly("address.city");
    }

    @Test
    @DisplayName("Wzorzec 2: koszyk — niepusta lista pozycji (bez null), sprawdzone pozycje i niepuste kupony")
    void exercise2() {
        Line good = new Line("A-1", 1);
        assertThat(validator.validate(new Basket(List.of(good), null))).isEmpty();
        assertThat(validator.validate(new Basket(List.of(good), List.of("PROMO10")))).isEmpty();

        assertThat(paths(validator.validate(new Basket(List.of(), null)))).containsExactly("lines");
        assertThat(paths(validator.validate(new Basket(List.of(good, new Line("A-2", 0)), null))))
                .containsExactly("lines[1].qty");
        assertThat(paths(validator.validate(new Basket(Arrays.asList(good, null), null))))
                .containsExactly("lines[1].<list element>");
        assertThat(paths(validator.validate(new Basket(List.of(good), List.of("PROMO10", " ")))))
                .containsExactly("coupons[1].<list element>");
    }

    @Test
    @DisplayName("Wzorzec 3: klient — id puste przy tworzeniu, obowiązkowe przy aktualizacji, e-mail zawsze")
    void exercise3() {
        assertThat(Val03NestedAndGroupsSolutions.solution3(validator, new Customer(null, "a@b.pl"), true)).isEmpty();
        assertThat(Val03NestedAndGroupsSolutions.solution3(validator, new Customer(7L, "a@b.pl"), true)).containsExactly("id");
        assertThat(Val03NestedAndGroupsSolutions.solution3(validator, new Customer(7L, " "), true)).containsExactly("email", "id");

        assertThat(Val03NestedAndGroupsSolutions.solution3(validator, new Customer(7L, "a@b.pl"), false)).isEmpty();
        assertThat(Val03NestedAndGroupsSolutions.solution3(validator, new Customer(null, "a@b.pl"), false)).containsExactly("id");
        assertThat(Val03NestedAndGroupsSolutions.solution3(validator, new Customer(null, ""), false)).containsExactly("email", "id");
    }
}
