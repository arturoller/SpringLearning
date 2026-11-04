package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintDefinitionException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Constraint;
import jakarta.validation.UnexpectedTypeException;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Val04CustomValidators. Najniższa wysokość: ZWYKŁY JUnit z czystą fabryką Validatora (bez Springa) —
 * własne ograniczenia są zwykłymi klasami Javy. Kontekst Springa jest potrzebny dopiero w sekcji 4 (Val04SpringTest).
 */
class Val04CustomValidatorsTest {

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

    private static <T> List<String> paths(Set<ConstraintViolation<T>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).sorted().toList();
    }

    private static <T> List<String> constraintNames(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .map(v -> v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()).sorted().toList();
    }

    // ------------------------------------------------------------------------------------------ sekcja 1

    @Test
    @DisplayName("Sekcja 1: własne ograniczenie @Sku przepuszcza ABC-1234 i null, odrzuca resztę")
    void customConstraintSku() {
        assertThat(validator.validate(new SkuForm("ABC-1234"))).isEmpty();
        assertThat(validator.validate(new SkuForm(null))).as("null = poprawne, od tego jest @NotNull").isEmpty();

        for (String bad : List.of("abc-1234", "ABC1234", "ABC-123", "ABC-12345", "ABC-1234 ", "")) {
            Set<ConstraintViolation<SkuForm>> violations = validator.validate(new SkuForm(bad));
            assertThat(paths(violations)).as("SKU '%s'", bad).containsExactly("sku");
            assertThat(constraintNames(violations)).containsExactly("Sku");
            assertThat(violations.iterator().next().getInvalidValue()).isEqualTo(bad);
        }
    }

    @Test
    @DisplayName("Sekcja 1: ograniczenie bez atrybutu payload (albo groups, message) kończy się ConstraintDefinitionException")
    void missingMandatoryAttribute() {
        record Item(@NoPayload String code) {
        }

        assertThatThrownBy(() -> validator.validate(new Item("x")))
                .isInstanceOf(ConstraintDefinitionException.class)
                .hasMessageContaining("payload");
    }

    @Test
    @DisplayName("Sekcja 1: ograniczenie na nieobsługiwanym typie (np. @Sku na Integer) daje UnexpectedTypeException")
    void wrongType() {
        record Broken(@Sku Integer number) {
        }

        assertThatThrownBy(() -> validator.validate(new Broken(5))).isInstanceOf(UnexpectedTypeException.class);
    }

    // ------------------------------------------------------------------------------------------ sekcja 2

    @Test
    @DisplayName("Sekcja 2: ograniczenie złożone z @ReportAsSingleViolation daje jedno naruszenie, bez niego — po jednym na składnik")
    void composedConstraints() {
        assertThat(validator.validate(new PhoneForms.Quiet("+48123456789"))).isEmpty();
        assertThat(validator.validate(new PhoneForms.Quiet("123456789"))).isEmpty();

        // null łamie @NotBlank (składnik), więc złożone ograniczenie — inaczej niż @Sku — wymaga wartości
        Set<ConstraintViolation<PhoneForms.Quiet>> quiet = validator.validate(new PhoneForms.Quiet("   "));
        assertThat(constraintNames(quiet)).as("jedno naruszenie, nazwane jak adnotacja złożona").containsExactly("PhoneNumber");
        assertThat(validator.validate(new PhoneForms.Quiet(null))).hasSize(1);

        Set<ConstraintViolation<PhoneForms.Loud>> loud = validator.validate(new PhoneForms.Loud("   "));
        assertThat(constraintNames(loud)).as("dwa naruszenia — po jednym na składnik").containsExactly("NotBlank", "Pattern");
    }

    // ------------------------------------------------------------------------------------------ sekcja 3

    @Test
    @DisplayName("Sekcja 3: walidator klasowy zgłasza błąd przypięty do pola end")
    void classLevelConstraint() {
        Promotion ok = new Promotion("Czarny piątek", LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 30));
        Promotion oneDay = new Promotion("Dzień kawy", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1));
        Promotion bad = new Promotion("Pomyłka", LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 27));

        assertThat(validator.validate(ok)).isEmpty();
        assertThat(validator.validate(oneDay)).isEmpty();

        Set<ConstraintViolation<Promotion>> violations = validator.validate(bad);
        assertThat(paths(violations)).as("ścieżka to pole 'end', nie pusta ścieżka klasy").containsExactly("end");
        assertThat(constraintNames(violations)).containsExactly("ValidDateRange");
        assertThat(violations.iterator().next().getRootBeanClass()).isEqualTo(Promotion.class);
    }

    @Test
    @DisplayName("Sekcja 3: wyłączone domyślne naruszenie bez żadnego własnego (brak addConstraintViolation) kończy się ValidationException HV000033")
    void contextPitfalls() {
        record Pair(@WithoutAdd String a) {
        }
        // walidator wyłącza domyślne naruszenie, buduje własne, ale zapomina o addConstraintViolation()
        assertThatThrownBy(() -> validator.validate(new Pair("cokolwiek")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("HV000033")
                .hasMessageContaining("At least one custom message must be created");
    }

    // ------------------------------------------------------------------------------------------ sekcja 4

    @Test
    @DisplayName("Sekcja 4: w czystej fabryce walidator z konstruktorem z argumentem nie powstaje (ValidationException)")
    void injectionDoesNotWorkInPlainFactory() {
        assertThatThrownBy(() -> validator.validate(new NewProduct("ABC-1234", "kawa")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unable to instantiate ConstraintValidator");
    }

    // ------------------------------------------------------------------------------------------ sekcja 5

    @Test
    @DisplayName("Sekcja 5: ValidationMessages.properties z korzenia classpath jest czytany jako UTF-8 (polskie litery)")
    void messagesFileIsUtf8() {
        String message = validator.validate(new MessageForms.ShortName("ab")).iterator().next().getMessage();

        assertThat(message).isEqualTo("Nazwa musi mieć co najmniej 3 znaki (zażółć gęślą jaźń)");   // {min} → 3
    }

    @Test
    @DisplayName("Sekcja 5: ${validatedValue} wstawia odrzuconą wartość, a {klucz} odsyła do pliku komunikatów")
    void validatedValueInMessage() {
        String message = validator.validate(new MessageForms.EchoSku("abc")).iterator().next().getMessage();

        assertThat(message).startsWith("Kod produktu musi mieć postać ABC-1234").endsWith("a podano: abc");
    }

    @Test
    @DisplayName("Sekcja 5: wyrażenie EL z wywołaniem metody jest w Hibernate Validator 9 wyłączone — komunikat zostaje surowy")
    void methodCallInMessageIsDisabled() {
        String message = validator.validate(new MessageForms.MethodCallInMessage("ab")).iterator().next().getMessage();

        assertThat(message).as("${validatedValue.length()} nie zostało obliczone")
                .isEqualTo("Podano ${validatedValue.length()} znaków, a minimum to 5");   // {min} (zwykły parametr) działa
    }

    @Test
    @DisplayName("Sekcja 5: formatter.format(...) w komunikacie działa, a \\${...} to dosłowny tekst")
    void formatterAndEscaping() {
        String formatted = validator.validate(new MessageForms.Formatted(new BigDecimal("5"))).iterator().next().getMessage();
        String escaped = validator.validate(new MessageForms.Escaped("a")).iterator().next().getMessage();

        assertThat(formatted).isEqualTo("Cena 5.00 jest za niska (min 10)");
        assertThat(escaped).isEqualTo("Wpisz ${wartość} (min 3)");
    }

    // ------------------------------------------------------------------------------------------ pomocnicze (tylko dla testów)

    /** Zepsuta adnotacja: brak obowiązkowego atrybutu payload. */
    @Constraint(validatedBy = AlwaysOk.class)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface NoPayload {
        String message() default "x";

        Class<?>[] groups() default {};
    }

    public static class AlwaysOk implements ConstraintValidator<NoPayload, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return true;
        }
    }

    /** Poprawna adnotacja, ale walidator wyłącza domyślne naruszenie i nie dodaje własnego. */
    @Constraint(validatedBy = ForgetfulValidator.class)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface WithoutAdd {
        String message() default "x";

        Class<?>[] groups() default {};

        Class<? extends jakarta.validation.Payload>[] payload() default {};
    }

    public static class ForgetfulValidator implements ConstraintValidator<WithoutAdd, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("nigdy nie zgłoszone").addPropertyNode("a");   // brak addConstraintViolation()
            return false;
        }
    }
}
