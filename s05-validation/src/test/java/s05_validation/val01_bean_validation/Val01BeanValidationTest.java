package s05_validation.val01_bean_validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.UnexpectedTypeException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import s05_validation.val01_bean_validation.Val01BeanValidation.BrokenQuantity;
import s05_validation.val01_bean_validation.Val01BeanValidation.MessageProbe;
import s05_validation.val01_bean_validation.Val01BeanValidation.NewProduct;
import s05_validation.val01_bean_validation.Val01BeanValidation.NullabilityProbe;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Val01BeanValidation — zwykłe testy jednostkowe, BEZ Springa. Dlaczego? Validator da się zbudować
 * samodzielnie, a test bez kontekstu działa w ułamku sekundy. Kontekst Springa jest tylko w Val01BeanValidationSpringTest.
 */
class Val01BeanValidationTest {

    static ValidatorFactory factory;   // factory = fabryka; budujemy raz na całą klasę (jest droga)
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

    /** Nazwy pól z naruszeniami — posortowane (Set naruszeń nie ma kolejności). */
    static Set<String> invalidFields(Object bean) {
        Set<String> names = new TreeSet<>();
        for (ConstraintViolation<Object> v : validator.validate(bean)) {
            names.add(v.getPropertyPath().toString());
        }
        return names;
    }

    static NewProduct product(String name, String code, int price, int stock, String email) {
        return new NewProduct(name, code, price, stock, email, LocalDate.of(2000, 1, 1), LocalDate.of(2999, 1, 1));
    }

    @Test
    @DisplayName("1. @NotNull, @NotEmpty i @NotBlank: tabela różnic dla null, \"\", \"   \" i \"x\"")
    void notNullNotEmptyNotBlank() {
        assertThat(invalidFields(new NullabilityProbe(null, null, null)))
                .containsExactly("notBlank", "notEmpty", "notNull");
        assertThat(invalidFields(new NullabilityProbe("", "", "")))
                .containsExactly("notBlank", "notEmpty");
        assertThat(invalidFields(new NullabilityProbe("   ", "   ", "   ")))
                .containsExactly("notBlank");
        assertThat(invalidFields(new NullabilityProbe("x", "x", "x"))).isEmpty();
    }

    @Test
    @DisplayName("2. Adnotacje na komponentach rekordu: poprawny produkt przechodzi, każdy błąd wskazuje swoje pole")
    void constraintsOnRecord() {
        assertThat(validator.validate(NewProduct.valid())).isEmpty();

        assertThat(invalidFields(product("ab", "MLK-0001", 349, 10, null))).containsExactly("name");        // @Size
        assertThat(invalidFields(product("Mleko", "mlk-0001", 349, 10, null))).containsExactly("code");     // @Pattern
        assertThat(invalidFields(product("Mleko", "MLK-0001", 0, 10, null))).containsExactly("priceGrosze"); // @Positive: 0 nie jest dodatnie
        assertThat(invalidFields(product("Mleko", "MLK-0001", 349, 1001, null))).containsExactly("stock");  // @Max
        assertThat(invalidFields(product("Mleko", "MLK-0001", 349, -1, null))).containsExactly("stock");    // @Min
        assertThat(invalidFields(product("Mleko", "MLK-0001", 349, 10, "to-nie-email"))).containsExactly("contactEmail");

        // @Pattern pasuje do CAŁEGO tekstu: kod z doklejonym końcem nie przechodzi
        assertThat(invalidFields(product("Mleko", "MLK-00012", 349, 10, null))).containsExactly("code");
        // @Email jest liberalny: domena bez kropki przechodzi
        assertThat(invalidFields(product("Mleko", "MLK-0001", 349, 10, "jan@localhost"))).isEmpty();

        // null przechodzi wszystkie adnotacje kształtu (@Size, @Email, @Past...) — łapie go tylko @NotBlank
        NewProduct withNulls = new NewProduct(null, null, 349, 10, null, null, null);
        assertThat(invalidFields(withNulls)).containsExactly("code", "name");
        // name: tylko @NotBlank zgłasza null (a @Size nie) — jedno naruszenie na pole
        assertThat(validator.validateProperty(withNulls, "name")).hasSize(1);
    }

    @Test
    @DisplayName("3. @Past i @Future: dalekie daty działają w każdym dniu, a stały zegar pozwala testować granicę „dziś”")
    void pastAndFuture() {
        // a) daty z zapasem — niezależne od dnia uruchomienia
        NewProduct wrongWayRound = new NewProduct("Mleko", "MLK-0001", 349, 10, null,
                LocalDate.of(2999, 1, 1), LocalDate.of(2000, 1, 1));
        assertThat(invalidFields(wrongWayRound)).containsExactly("bestBefore", "producedOn");

        // b) ClockProvider ze stałym zegarem: „teraz” = 2026-10-11 (UTC)
        Clock fixed = Clock.fixed(Instant.parse("2026-10-11T10:00:00Z"), ZoneOffset.UTC);
        try (ValidatorFactory fixedFactory = Validation.byDefaultProvider().configure()
                .clockProvider(() -> fixed).buildValidatorFactory()) {
            Validator fixedValidator = fixedFactory.getValidator();
            record Dates(@Past LocalDate produced, @Future LocalDate bestBefore) {
            }
            // dziś NIE jest ani przeszłością, ani przyszłością
            assertThat(fixedValidator.validate(new Dates(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 11)))).hasSize(2);
            assertThat(fixedValidator.validate(new Dates(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12)))).isEmpty();
        }
    }

    @Test
    @DisplayName("4. ConstraintViolation niesie ścieżkę pola, wartość, szablon komunikatu i adnotację; walidator zbiera WSZYSTKIE błędy")
    void violationApi() {
        Set<ConstraintViolation<NewProduct>> one = validator.validate(product("ab", "MLK-0001", 349, 10, null));
        assertThat(one).hasSize(1);
        ConstraintViolation<NewProduct> v = one.iterator().next();
        assertThat(v.getPropertyPath().toString()).isEqualTo("name");
        assertThat(v.getInvalidValue()).isEqualTo("ab");
        assertThat(v.getMessage()).isEqualTo("Nazwa ma mieć od 3 do 40 znaków");   // własny komunikat — nie zależy od języka
        assertThat(v.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Size.class);
        assertThat(v.getConstraintDescriptor().getAttributes()).containsEntry("min", 3).containsEntry("max", 40);

        // wiele błędów naraz — walidator nie zatrzymuje się na pierwszym
        Set<ConstraintViolation<NewProduct>> many = validator.validate(product("ab", "zly", -5, 5000, "x"));
        assertThat(many).hasSize(5);
    }

    @Test
    @DisplayName("5. Komunikaty: własny tekst, podstawianie {min}/{max}, a domyślny tekst zależy od języka")
    void messages() {
        // własny komunikat z adnotacji
        ConstraintViolation<NewProduct> blank = validator.validate(product("   ", "MLK-0001", 349, 10, null)).iterator().next();
        assertThat(blank.getMessage()).isEqualTo("Nazwa produktu jest wymagana");
        assertThat(blank.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotBlank.class);

        // jawnie ustawiony język → deterministyczny domyślny komunikat, niezależny od komputera
        MessageProbe probe = new MessageProbe("", "x");
        assertThat(messagesFor(probe, Locale.ENGLISH))
                .containsEntry("plain", "must not be blank")
                .containsEntry("templated", "od 2 do 5 znaków");           // {min} i {max} podstawione
        assertThat(messagesFor(probe, Locale.forLanguageTag("pl")))
                .containsEntry("plain", "nie może być odstępem");           // ten sam błąd, inny język
    }

    private static java.util.Map<String, String> messagesFor(Object bean, Locale locale) {
        try (ValidatorFactory f = Validation.byProvider(HibernateValidator.class).configure()
                .defaultLocale(locale).locales(locale).buildValidatorFactory()) {
            java.util.Map<String, String> result = new java.util.TreeMap<>();
            f.getValidator().validate(bean).forEach(v -> result.put(v.getPropertyPath().toString(), v.getMessage()));
            return result;
        }
    }

    @Test
    @DisplayName("6. Adnotacja niepasująca do typu pola (@Size na Integer) kończy się UnexpectedTypeException")
    void unsupportedType() {
        assertThatThrownBy(() -> validator.validate(new BrokenQuantity(5)))
                .isInstanceOf(UnexpectedTypeException.class)
                .hasMessageContaining("HV000030");
    }
}
