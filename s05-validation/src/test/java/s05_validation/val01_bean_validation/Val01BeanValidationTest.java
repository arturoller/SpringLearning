package s05_validation.val01_bean_validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.UnexpectedTypeException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Val01BeanValidation. Najniższa wysokość: ZWYKŁY JUnit, bez Springa — Validator to czysta Java, więc
 * kontekst Springa byłby tu tylko stratą czasu. Fabrykę tworzymy raz na klasę i zamykamy w {@code @AfterAll}.
 * Asercje opieramy na rzeczach stałych (ścieżka, typ adnotacji, wartość), nie na treści komunikatu (zależy od Locale).
 */
class Val01BeanValidationTest {

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

    /** Pomocnik: naruszenia jako posortowane napisy "ścieżka:Adnotacja". */
    static List<String> described(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath() + ":" + v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
                .sorted()
                .collect(Collectors.toList());
    }

    private static ProductForm validProduct() {
        return new ProductForm("Kawa", new BigDecimal("24.90"), 5, null);
    }

    // ---------------------------------------------------------------- 1

    @Test
    @DisplayName("Sekcja 1: Validator pochodzi z Hibernate Validatora, a poprawny formularz nie ma naruszeń")
    void specificationAndImplementation() {
        assertThat(validator.getClass().getName()).startsWith("org.hibernate.validator.");
        assertThat(validator.validate(validProduct())).isEmpty();
        assertThat(validator.validate(new CustomerForm("ala@example.com", null, null, null))).isEmpty();
    }

    // ---------------------------------------------------------------- 2

    record NotNullHolder(@NotNull String value) {
    }

    record NotEmptyHolder(@NotEmpty String value) {
    }

    record NotBlankHolder(@NotBlank String value) {
    }

    @Test
    @DisplayName("Sekcja 2: tabela @NotNull / @NotEmpty / @NotBlank dla null, \"\", \"  \" i \"ala\"")
    void notNullNotEmptyNotBlank() {
        String[] values = {null, "", "  ", "ala"};
        boolean[] notNullFails = {true, false, false, false};
        boolean[] notEmptyFails = {true, true, false, false};
        boolean[] notBlankFails = {true, true, true, false};
        for (int i = 0; i < values.length; i++) {
            assertThat(validator.validate(new NotNullHolder(values[i])).isEmpty()).isEqualTo(!notNullFails[i]);
            assertThat(validator.validate(new NotEmptyHolder(values[i])).isEmpty()).isEqualTo(!notEmptyFails[i]);
            assertThat(validator.validate(new NotBlankHolder(values[i])).isEmpty()).isEqualTo(!notBlankFails[i]);
        }
    }

    record NotBlankOnNumber(@NotBlank Integer value) {
    }

    @Test
    @DisplayName("Sekcja 2: @NotBlank na liczbie to UnexpectedTypeException (działa tylko na tekście)")
    void notBlankOnNumber() {
        assertThatThrownBy(() -> validator.validate(new NotBlankOnNumber(5)))
                .isInstanceOf(UnexpectedTypeException.class);
    }

    @Test
    @DisplayName("Sekcja 2: ProductForm — każda reguła z katalogu łamana osobno (pole, adnotacja, wartość)")
    void productConstraints() {
        assertThat(described(validator.validate(new ProductForm("Kawa", new BigDecimal("0.00"), 5, null))))
                .containsExactly("price:DecimalMin");
        assertThat(described(validator.validate(new ProductForm("Kawa", new BigDecimal("1.234"), 5, null))))
                .containsExactly("price:Digits");
        assertThat(described(validator.validate(new ProductForm("Kawa", new BigDecimal("1234567.12"), 5, null))))
                .containsExactly("price:Digits");
        assertThat(described(validator.validate(new ProductForm("Kawa", new BigDecimal("24.90"), 0, null))))
                .containsExactly("quantity:Min");
        assertThat(described(validator.validate(new ProductForm("Kawa", new BigDecimal("24.90"), 1001, null))))
                .containsExactly("quantity:Max");
        assertThat(described(validator.validate(new ProductForm("Kawa", new BigDecimal("24.90"), 5, -1))))
                .containsExactly("discountPercent:PositiveOrZero");
        assertThat(described(validator.validate(new ProductForm("ab", new BigDecimal("24.90"), 5, null))))
                .containsExactly("name:Size");
        assertThat(described(validator.validate(new ProductForm("x".repeat(31), new BigDecimal("24.90"), 5, null))))
                .containsExactly("name:Size");
        // granice są włącznie: 3 i 30 znaków, ilość 1 i 1000, zniżka 0, cena 0,01 — wszystko poprawne
        assertThat(validator.validate(new ProductForm("abc", new BigDecimal("0.01"), 1, 0))).isEmpty();
        assertThat(validator.validate(new ProductForm("x".repeat(30), new BigDecimal("999999.99"), 1000, 100))).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 2: naruszenie zna ścieżkę, adnotację i odrzuconą wartość")
    void violationDetails() {
        ConstraintViolation<ProductForm> violation = validator
                .validate(new ProductForm("Kawa", new BigDecimal("24.90"), 1001, null)).iterator().next();

        assertThat(violation.getPropertyPath().toString()).isEqualTo("quantity");
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(jakarta.validation.constraints.Max.class);
        assertThat(violation.getInvalidValue()).isEqualTo(1001);
        assertThat(violation.getRootBeanClass()).isEqualTo(ProductForm.class);
    }

    @Test
    @DisplayName("Sekcja 2: walidacja nie staje na pierwszym błędzie — jedno pole może dostać kilka naruszeń")
    void severalViolations() {
        assertThat(described(validator.validate(new ProductForm("  ", new BigDecimal("24.90"), 5, null))))
                .containsExactly("name:NotBlank", "name:Size");
        assertThat(described(validator.validate(new ProductForm("  ", new BigDecimal("0.001"), 0, -1))))
                .containsExactly("discountPercent:PositiveOrZero", "name:NotBlank", "name:Size", "price:DecimalMin",
                        "price:Digits", "quantity:Min");
        // brak danych: null łamie tylko te ograniczenia, które go zabraniają (@NotBlank, @NotNull)
        assertThat(described(validator.validate(new ProductForm(null, null, null, null))))
                .containsExactly("name:NotBlank", "price:NotNull", "quantity:NotNull");
    }

    @Test
    @DisplayName("Sekcja 2: CustomerForm — @Email, @Pattern, @Past i @Future")
    void customerConstraints() {
        assertThat(described(validator.validate(new CustomerForm("ala ma kota", null, null, null))))
                .containsExactly("email:Email");
        assertThat(described(validator.validate(new CustomerForm(null, null, null, null))))
                .containsExactly("email:NotBlank");
        assertThat(described(validator.validate(new CustomerForm("ala@example.com", "123", null, null))))
                .containsExactly("phone:Pattern");
        assertThat(validator.validate(new CustomerForm("ala@example.com", "+48123456789", null, null))).isEmpty();
        // "Pattern" dopasowuje CAŁY tekst: 9 cyfr + litera na końcu nie przejdzie
        assertThat(described(validator.validate(new CustomerForm("ala@example.com", "123456789x", null, null))))
                .containsExactly("phone:Pattern");
        // daty z odległej przeszłości i przyszłości — wynik nie zależy od dnia, w którym biegnie test
        assertThat(validator.validate(new CustomerForm("ala@example.com", null, LocalDate.of(1990, 1, 1), LocalDate.of(2999, 1, 1)))).isEmpty();
        assertThat(described(validator.validate(new CustomerForm("ala@example.com", null, LocalDate.of(2999, 1, 1), LocalDate.of(1990, 1, 1)))))
                .containsExactly("birthDate:Past", "cardValidUntil:Future");
    }

    @Test
    @DisplayName("Sekcja 2: validateValue i validateProperty sprawdzają jedną właściwość")
    void singleProperty() {
        assertThat(validator.validateValue(ProductForm.class, "price", new BigDecimal("0")))
                .extracting(v -> v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
                .containsExactly("DecimalMin");
        assertThat(validator.validateValue(ProductForm.class, "price", new BigDecimal("1.50"))).isEmpty();
        // validateProperty sprawdza właściwość istniejącego obiektu; reszta formularza (tu: błędna ilość) jest pomijana
        assertThat(described(validator.validateProperty(new ProductForm("  ", null, 0, null), "name")))
                .containsExactly("name:NotBlank", "name:Size");
    }

    // ---------------------------------------------------------------- 3

    @Test
    @DisplayName("Sekcja 3: rekord — adnotacja przy komponencie działa i daje jedno naruszenie (nie trzy)")
    void recordComponents() {
        Set<ConstraintViolation<ProductForm>> violations = validator.validate(new ProductForm("", new BigDecimal("24.90"), 5, null));

        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsOnly("name");
        // "" łamie @NotBlank i @Size(min = 3) — dokładnie dwa naruszenia, nie po jednym na pole/akcesor/konstruktor
        assertThat(violations).hasSize(2);
    }

    @Test
    @DisplayName("Sekcja 3: zwykła klasa — adnotacja na polu (name) i na getterze (nickname) daje ścieżkę nazwaną jak właściwość")
    void fieldAndGetter() {
        assertThat(described(validator.validate(new LegacyCustomer(" ", "x"))))
                .containsExactly("name:NotBlank", "nickname:Size");
        assertThat(validator.validate(new LegacyCustomer("Ala", "Al"))).isEmpty();
    }

    // ---------------------------------------------------------------- 5

    @Test
    @DisplayName("Sekcja 5: @NotNull na int nie robi nic — brak danych to 0, a 0 nie jest null")
    void notNullOnPrimitive() {
        assertThat(validator.validate(new TrapForms.PrimitiveQuantity(0))).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 5: @Size na Integer to UnexpectedTypeException — także dla null")
    void sizeOnNumber() {
        assertThatThrownBy(() -> validator.validate(new TrapForms.SizeOnNumber(3)))
                .isInstanceOf(UnexpectedTypeException.class)
                .hasMessageContaining("Size")
                .hasMessageContaining("Integer");
        assertThatThrownBy(() -> validator.validate(new TrapForms.SizeOnNumber(null)))
                .isInstanceOf(UnexpectedTypeException.class);
    }

    @Test
    @DisplayName("Sekcja 5: @Email przepuszcza null i adres \"a@b\"; para @NotNull @Email wyłapuje null")
    void emailPitfalls() {
        assertThat(validator.validate(new TrapForms.EmailOnly(null))).isEmpty();
        assertThat(validator.validate(new TrapForms.EmailOnly("a@b"))).isEmpty();
        assertThat(described(validator.validate(new TrapForms.EmailOnly("@b.pl")))).containsExactly("email:Email");
        assertThat(described(validator.validate(new TrapForms.EmailRequired(null)))).containsExactly("email:NotNull");
    }

    // ---------------------------------------------------------------- 6

    private static Validator validatorFor(Locale locale) {
        HibernateValidatorConfiguration configuration = Validation.byProvider(HibernateValidator.class).configure();
        configuration.defaultLocale(locale);
        return configuration.buildValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Sekcja 6: domyślny komunikat zależy od Locale — ustawiamy go jawnie, więc test jest deterministyczny")
    void defaultMessageDependsOnLocale() {
        ProductForm blank = new ProductForm("", new BigDecimal("24.90"), 5, null);
        String english = messageOf(validatorFor(Locale.ENGLISH).validate(blank), "NotBlank");
        String polish = messageOf(validatorFor(Locale.of("pl")).validate(blank), "NotBlank");

        assertThat(english).isEqualTo("must not be blank");
        assertThat(polish).isNotEqualTo(english).isNotBlank();
    }

    private static String messageOf(Set<ConstraintViolation<ProductForm>> violations, String annotationName) {
        return violations.stream()
                .filter(v -> v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName().equals(annotationName))
                .findFirst().orElseThrow().getMessage();
    }

    @Test
    @DisplayName("Sekcja 6: własny komunikat z {regexp} jest po polsku i nie zależy od Locale")
    void customMessageWithAttribute() {
        for (Locale locale : List.of(Locale.ENGLISH, Locale.of("pl"))) {
            Set<ConstraintViolation<CustomerForm>> violations =
                    validatorFor(locale).validate(new CustomerForm("ala@example.com", "123", null, null));

            assertThat(violations).singleElement()
                    .extracting(ConstraintViolation::getMessage)
                    .isEqualTo("Telefon musi pasować do wzorca \\+?[0-9]{9,12}");
        }
    }

    @Test
    @DisplayName("Sekcja 6: klucz {val01.name.short} jest czytany z pliku zasobów w UTF-8, a {min} podstawia 3")
    void messageFromResourceBundle() {
        HibernateValidatorConfiguration configuration = Validation.byProvider(HibernateValidator.class).configure();
        configuration.messageInterpolator(new ResourceBundleMessageInterpolator(
                new PlatformResourceBundleLocator("val01.Val01Messages")));
        try (ValidatorFactory bundleFactory = configuration.buildValidatorFactory()) {
            Set<ConstraintViolation<MessageForm>> violations = bundleFactory.getValidator().validate(new MessageForm("ab"));

            assertThat(violations).singleElement().extracting(ConstraintViolation::getMessage)
                    .isEqualTo("Nazwa musi mieć co najmniej 3 znaki (zażółć gęślą jaźń)");
        }
    }
}
