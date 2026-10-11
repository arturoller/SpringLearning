package s05_validation.val04_custom_validators;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Payload;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import s05_validation.val04_custom_validators.Val04Types.CatalogProduct;
import s05_validation.val04_custom_validators.Val04Types.Item;
import s05_validation.val04_custom_validators.Val04Types.LongCodeItem;
import s05_validation.val04_custom_validators.Val04Types.Promotion;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Val04CustomValidators — zwykłe testy jednostkowe z samym Validatorem (bez Springa). Walidator z beanem
 * sprawdza Val04CustomValidatorsSpringTest, a kontroler Val04CustomValidatorsWebMvcTest.
 */
class Val04CustomValidatorsTest {

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

    /** Ścieżki naruszeń posortowane; pusta ścieżka (cały obiekt) jest zapisana jako "". */
    static List<String> paths(Set<? extends ConstraintViolation<?>> violations) {
        List<String> result = new ArrayList<>();
        violations.forEach(v -> result.add(v.getPropertyPath().toString()));
        result.sort(String::compareTo);
        return result;
    }

    // --- lokalne ograniczenia do pokazania pułapek (nie są częścią lekcji, tylko eksperymentem) ---------

    /** Adnotacja BEZ @Retention(RUNTIME): domyślnie CLASS, więc w czasie działania jej nie widać. */
    @Constraint(validatedBy = NoRuntimeValidator.class)
    @Target(FIELD)
    @interface NoRuntime {
        String message() default "zawsze błąd";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    /** To samo, ale z @Retention(RUNTIME) — dla porównania. */
    @Constraint(validatedBy = AlwaysFailsValidator.class)
    @Target(FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface WithRuntime {
        String message() default "zawsze błąd";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class AlwaysFailsValidator implements ConstraintValidator<WithRuntime, Object> {
        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            return false;
        }
    }

    record RuntimeProbe(@WithRuntime String a) {
    }

    public static class NoRuntimeValidator implements ConstraintValidator<NoRuntime, Object> {
        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            return false;
        }
    }

    /** Reguła klasowa „niedbała”: NIE wyłącza komunikatu domyślnego i dodaje własny na polu. */
    @Constraint(validatedBy = SloppyValidator.class)
    @Target(TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sloppy {
        String message() default "domyślny na całości";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class SloppyValidator implements ConstraintValidator<Sloppy, Object> {
        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            context.buildConstraintViolationWithTemplate("własny na polu").addPropertyNode("to").addConstraintViolation();
            return false;
        }
    }

    @Sloppy
    record SloppyRange(int from, int to) {
    }

    /** Reguła klasowa, która wyłącza komunikat domyślny, buduje własny, ale NIE wywołuje addConstraintViolation(). */
    @Constraint(validatedBy = ForgetfulValidator.class)
    @Target(TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Forgetful {
        String message() default "domyślny na całości";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class ForgetfulValidator implements ConstraintValidator<Forgetful, Object> {
        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("własny na polu").addPropertyNode("to");   // brak addConstraintViolation()!
            return false;
        }
    }

    @Forgetful
    record ForgetfulRange(int from, int to) {
    }

    // --- testy ------------------------------------------------------------------------------------------

    @Test
    @DisplayName("1. @ProductCode: poprawny kod przechodzi, zły kształt nie; atrybut digits zmienia długość; bez RUNTIME reguła nie działa")
    void productCodeConstraint() {
        assertThat(validator.validate(new Item("ABC-1234", "ABC-1234"))).isEmpty();
        assertThat(paths(validator.validate(new Item("abc-1234", "ABC-123")))).containsExactly("optionalCode", "requiredCode");
        assertThat(paths(validator.validate(new Item("ABC-12345", "ABC-1234")))).containsExactly("optionalCode");

        // atrybut digits = 6
        assertThat(validator.validate(new LongCodeItem("ABC-123456"))).isEmpty();
        ConstraintViolation<LongCodeItem> violation = validator.validate(new LongCodeItem("ABC-1234")).iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Kod produktu: trzy wielkie litery, myślnik i 6 cyfr");   // {digits} podstawione

        // PUŁAPKA: adnotacja bez @Retention(RUNTIME) — brak błędu, brak reguły
        record NoRuntimeProbe(@NoRuntime String a) {
        }
        assertThat(validator.validate(new NoRuntimeProbe("x"))).isEmpty();
        assertThat(validator.validate(new RuntimeProbe("x"))).hasSize(1);   // ta sama reguła z RUNTIME działa
    }

    @Test
    @DisplayName("2. Konwencja null: sam @ProductCode przepuszcza null, z @NotNull pole jest obowiązkowe")
    void nullConvention() {
        Set<ConstraintViolation<Item>> violations = validator.validate(new Item(null, null));
        assertThat(paths(violations)).containsExactly("requiredCode");
        assertThat(violations.iterator().next().getConstraintDescriptor().getAnnotation().annotationType())
                .isEqualTo(jakarta.validation.constraints.NotNull.class);
    }

    @Test
    @DisplayName("3. Reguła klasowa @ValidPromotion: błędy przypięte do pól validTo i discountGrosze")
    void classLevelConstraint() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        assertThat(validator.validate(new Promotion(from, to, 1000, 300))).isEmpty();
        assertThat(validator.validate(new Promotion(from, from, 1000, 1000))).isEmpty();   // granice włącznie

        assertThat(paths(validator.validate(new Promotion(to, from, 1000, 300)))).containsExactly("validTo");
        assertThat(paths(validator.validate(new Promotion(from, to, 1000, 1001)))).containsExactly("discountGrosze");
        assertThat(paths(validator.validate(new Promotion(to, from, 1000, 1001)))).containsExactly("discountGrosze", "validTo");

        ConstraintViolation<Promotion> v = validator.validate(new Promotion(to, from, 1000, 300)).iterator().next();
        assertThat(v.getMessage()).isEqualTo("Data końca nie może być przed datą początku");

        // PUŁAPKA 1: bez disableDefaultConstraintViolation() — dwa naruszenia (domyślne na całości i własne na polu)
        assertThat(paths(validator.validate(new SloppyRange(1, 2)))).containsExactly("", "to");

        // PUŁAPKA 2: komunikat domyślny wyłączony, ale brak addConstraintViolation() — Hibernate Validator zgłasza błąd
        assertThatThrownBy(() -> validator.validate(new ForgetfulRange(1, 2)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("HV000033");
    }

    @Test
    @DisplayName("4a. Zwykły Validator (bez Springa) nie potrafi utworzyć walidatora z konstruktorem z parametrem")
    void plainFactoryCannotInject() {
        assertThatThrownBy(() -> validator.validate(new CatalogProduct("ABC-1234", "AGD")))
                .isInstanceOf(ValidationException.class);
    }
}
