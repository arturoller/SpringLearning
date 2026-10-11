package s05_validation.val01_bean_validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s05_validation.val01_bean_validation.Val01BeanValidation.Exercise1Customer;
import s05_validation.val01_bean_validation.Val01BeanValidation.Exercise3Employee;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Val01BeanValidation. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Val01BeanValidationExercisesTest {

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

    static Set<String> fields(Object bean) {
        Set<String> names = new TreeSet<>();
        for (ConstraintViolation<Object> v : validator.validate(bean)) {
            names.add(v.getPropertyPath().toString());
        }
        return names;
    }

    @Test
    @DisplayName("Ćwiczenie 1: klient — nazwa, e-mail i wiek (min. 18)")
    void exercise1() {
        assertThat(fields(new Exercise1Customer("Anna", "anna@sklep.pl", 30))).isEmpty();
        assertThat(fields(new Exercise1Customer("   ", "anna@sklep.pl", 30))).containsExactly("name");
        assertThat(fields(new Exercise1Customer("Anna", "to-nie-email", 30))).containsExactly("email");
        assertThat(fields(new Exercise1Customer("Anna", null, 30))).containsExactly("email");
        assertThat(fields(new Exercise1Customer("Anna", "anna@sklep.pl", 17))).containsExactly("age");
    }

    @Test
    @DisplayName("Ćwiczenie 2: lista „pole: komunikat” posortowana")
    void exercise2() {
        record Item(@NotBlank(message = "Nazwa jest wymagana") String name,
                    @Positive(message = "Cena musi być dodatnia") int price) {
        }
        assertThat(Val01BeanValidation.exercise2(validator, new Item(" ", -1)))
                .containsExactly("name: Nazwa jest wymagana", "price: Cena musi być dodatnia");
        assertThat(Val01BeanValidation.exercise2(validator, new Item("Chleb", 5))).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: pracownik — login, pensja i data zatrudnienia z dokładnymi komunikatami")
    void exercise3() {
        LocalDate past = LocalDate.of(2000, 1, 1);
        assertThat(fields(new Exercise3Employee("kowalski", 500_000, past))).isEmpty();

        assertThat(messages(new Exercise3Employee("ab", 500_000, past))).containsExactly("login: Login: 3-12 znaków");
        assertThat(messages(new Exercise3Employee("Kowalski", 500_000, past))).containsExactly("login: Login: tylko małe litery");
        assertThat(messages(new Exercise3Employee(null, 500_000, past))).hasSize(1);
        assertThat(messages(new Exercise3Employee("kowalski", 0, past))).containsExactly("salary: Pensja: od 1 do 1000000");
        assertThat(messages(new Exercise3Employee("kowalski", 1_000_001, past))).containsExactly("salary: Pensja: od 1 do 1000000");
        assertThat(messages(new Exercise3Employee("kowalski", 500_000, LocalDate.of(2999, 1, 1))))
                .containsExactly("hiredOn: Data zatrudnienia musi być z przeszłości");
    }

    private static Set<String> messages(Object bean) {
        Set<String> result = new TreeSet<>();
        for (ConstraintViolation<Object> v : validator.validate(bean)) {
            result.add(v.getPropertyPath() + ": " + v.getMessage());
        }
        return result;
    }
}
