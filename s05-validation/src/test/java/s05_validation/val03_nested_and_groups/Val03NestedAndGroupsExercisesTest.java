package s05_validation.val03_nested_and_groups;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s05_validation.val03_nested_and_groups.Val03NestedAndGroups.Exercise1Address;
import s05_validation.val03_nested_and_groups.Val03NestedAndGroups.Exercise1Shipment;
import s05_validation.val03_nested_and_groups.Val03NestedAndGroups.Exercise2Basket;
import s05_validation.val03_nested_and_groups.Val03NestedAndGroups.Exercise2Line;
import s05_validation.val03_nested_and_groups.Val03NestedAndGroups.Exercise3Customer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Val03NestedAndGroups. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Val03NestedAndGroupsExercisesTest {

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
    @DisplayName("Ćwiczenie 1: przesyłka — odbiorca i obowiązkowy adres sprawdzany w głąb")
    void exercise1() {
        assertThat(validator.validate(new Exercise1Shipment("Ala", new Exercise1Address("Gdańsk")))).isEmpty();
        assertThat(paths(validator.validate(new Exercise1Shipment(" ", new Exercise1Address("Gdańsk")))))
                .containsExactly("receiver");
        assertThat(paths(validator.validate(new Exercise1Shipment("Ala", null)))).containsExactly("address");
        assertThat(paths(validator.validate(new Exercise1Shipment("Ala", new Exercise1Address(""))))).containsExactly("address.city");
    }

    @Test
    @DisplayName("Ćwiczenie 2: koszyk — niepusta lista pozycji (bez null), sprawdzone pozycje i niepuste kupony")
    void exercise2() {
        Exercise2Line good = new Exercise2Line("A-1", 1);
        assertThat(validator.validate(new Exercise2Basket(List.of(good), null))).isEmpty();
        assertThat(validator.validate(new Exercise2Basket(List.of(good), List.of("PROMO10")))).isEmpty();

        assertThat(paths(validator.validate(new Exercise2Basket(List.of(), null)))).containsExactly("lines");
        assertThat(paths(validator.validate(new Exercise2Basket(List.of(good, new Exercise2Line("A-2", 0)), null))))
                .containsExactly("lines[1].qty");
        assertThat(paths(validator.validate(new Exercise2Basket(Arrays.asList(good, null), null))))
                .containsExactly("lines[1].<list element>");
        assertThat(paths(validator.validate(new Exercise2Basket(List.of(good), List.of("PROMO10", " ")))))
                .containsExactly("coupons[1].<list element>");
    }

    @Test
    @DisplayName("Ćwiczenie 3: klient — id puste przy tworzeniu, obowiązkowe przy aktualizacji, e-mail zawsze")
    void exercise3() {
        assertThat(Val03NestedAndGroups.exercise3(validator, new Exercise3Customer(null, "a@b.pl"), true)).isEmpty();
        assertThat(Val03NestedAndGroups.exercise3(validator, new Exercise3Customer(7L, "a@b.pl"), true)).containsExactly("id");
        assertThat(Val03NestedAndGroups.exercise3(validator, new Exercise3Customer(7L, " "), true)).containsExactly("email", "id");

        assertThat(Val03NestedAndGroups.exercise3(validator, new Exercise3Customer(7L, "a@b.pl"), false)).isEmpty();
        assertThat(Val03NestedAndGroups.exercise3(validator, new Exercise3Customer(null, "a@b.pl"), false)).containsExactly("id");
        assertThat(Val03NestedAndGroups.exercise3(validator, new Exercise3Customer(null, ""), false)).containsExactly("email", "id");
    }
}
