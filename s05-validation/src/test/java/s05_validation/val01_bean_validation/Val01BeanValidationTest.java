package s05_validation.val01_bean_validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Val01BeanValidation. {@code @SpringBootTest} — potrzebujemy bean Validator z kontekstu Springa
 * (autokonfiguracja walidacji z Boota); kontroler nie jest potrzebny, więc nie ma MockMvc.
 */
@SpringBootTest
class Val01BeanValidationTest {

    @Autowired
    Validator validator;   // jakarta.validation.Validator — bean "defaultValidator" z Boota

    @Autowired
    ApplicationContext context;

    /** Zamienia naruszenia na posortowany zbiór nazw pól — kolejność w Set naruszeń nie jest gwarantowana. */
    private static <T> Set<String> paths(Set<ConstraintViolation<T>> violations) {
        Set<String> result = new TreeSet<>();
        violations.forEach(v -> result.add(v.getPropertyPath().toString()));
        return result;
    }

    @Test
    @DisplayName("1. Spring Boot udostępnia bean Validator: defaultValidator typu LocalValidatorFactoryBean")
    void section1_validatorBean() {
        assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
        assertThat(validator).isInstanceOf(LocalValidatorFactoryBean.class);
        // ta sama klasa jest też springowym Validatorem (org.springframework.validation.Validator)
        assertThat(validator).isInstanceOf(org.springframework.validation.Validator.class);
    }

    @Test
    @DisplayName("2. Poprawny formularz → pusty zbiór; zły → naruszenia na właściwych polach z właściwymi regułami")
    void section2_basicConstraints() {
        ProductForm ok = new ProductForm("Kawa ziarnista 1kg", "SPO-001", new BigDecimal("64.99"), 120, "Arabica");
        assertThat(validator.validate(ok)).isEmpty();   // nigdy null — pusty zbiór

        ProductForm bad = new ProductForm(" ", "abc", null, 5000, "ab");
        Set<ConstraintViolation<ProductForm>> violations = validator.validate(bad);

        assertThat(paths(violations)).containsExactly("description", "name", "price", "sku", "stock");

        // pole → typ adnotacji, która zgłosiła naruszenie (stabilne, w przeciwieństwie do treści komunikatu)
        Map<String, String> annotationByField = new TreeMap<>();
        violations.forEach(v -> annotationByField.put(v.getPropertyPath().toString(),
                v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()));
        assertThat(annotationByField).containsExactly(
                Map.entry("description", "Size"),
                Map.entry("name", "NotBlank"),
                Map.entry("price", "NotNull"),
                Map.entry("sku", "Pattern"),
                Map.entry("stock", "Max"));
    }

    @Test
    @DisplayName("3. @NotNull, @NotEmpty, @NotBlank i @Size inaczej traktują null, pusty tekst i spacje")
    void section3_notNullNotEmptyNotBlank() {
        String[] values = {null, "", " ", "x", "xy"};
        // liczba naruszeń: wiersze = wartości, kolumny = notNull, notEmpty, notBlank, size (min 2)
        int[][] expected = {
                {1, 1, 1, 0},   // null
                {0, 1, 1, 1},   // ""
                {0, 0, 1, 1},   // " "
                {0, 0, 0, 1},   // "x"
                {0, 0, 0, 0}};  // "xy"
        String[] fields = {"notNull", "notEmpty", "notBlank", "size"};

        for (int row = 0; row < values.length; row++) {
            for (int col = 0; col < fields.length; col++) {
                int violations = validator.validateValue(NullEmptyBlank.class, fields[col], values[row]).size();
                assertThat(violations)
                        .as("pole %s, wartość [%s]", fields[col], values[row])
                        .isEqualTo(expected[row][col]);
            }
        }
    }

    @Test
    @DisplayName("4. null jest poprawny dla @Size, @Email, @Past, @Future; @Email jest łagodny")
    void section4_nullIsValid() {
        // pułapka z lekcji (sekcja 4): samo @Size(min = 3) nie wymusza opisu
        ProductForm noDescription = new ProductForm("Kawa", "SPO-001", BigDecimal.ONE, null, null);
        assertThat(validator.validate(noDescription)).isEmpty();   // stock = null i description = null

        assertThat(validator.validateValue(CustomerForm.class, "email", null)).isEmpty();
        assertThat(validator.validateValue(CustomerForm.class, "email", "")).isEmpty();
        assertThat(validator.validateValue(CustomerForm.class, "email", "jan@example")).isEmpty();   // łagodny format
        assertThat(validator.validateValue(CustomerForm.class, "email", "jan@")).hasSize(1);
        assertThat(validator.validateValue(CustomerForm.class, "email", "abc")).hasSize(1);

        // pytanie kontrolne 4 dosłownie
        assertThat(validator.validate(new CustomerForm("Jan", "jan@example", null, null))).isEmpty();

        // daty: odległe, żeby test nie zależał od zegara
        assertThat(validator.validateValue(CustomerForm.class, "birthDate", LocalDate.of(1990, 1, 1))).isEmpty();
        assertThat(validator.validateValue(CustomerForm.class, "birthDate", LocalDate.of(2999, 1, 1))).hasSize(1);
        assertThat(validator.validateValue(CustomerForm.class, "nextVisit", LocalDate.of(2999, 1, 1))).isEmpty();
        assertThat(validator.validateValue(CustomerForm.class, "nextVisit", LocalDate.of(1990, 1, 1))).hasSize(1);
    }

    @Test
    @DisplayName("5. ConstraintViolation: ścieżka pola, zła wartość, szablon komunikatu, klasa obiektu")
    void section5_constraintViolation() {
        ProductForm form = new ProductForm("Kawa", "zle", BigDecimal.ONE, 1, null);

        ConstraintViolation<ProductForm> violation = validator.validate(form).iterator().next();

        assertThat(violation.getPropertyPath()).hasToString("sku");
        assertThat(violation.getInvalidValue()).isEqualTo("zle");
        assertThat(violation.getRootBeanClass()).isEqualTo(ProductForm.class);
        assertThat(violation.getMessageTemplate()).isEqualTo("SKU ma format np. ELE-001");   // szablon bez klamer = ten sam tekst

        // dla reguły z komunikatem domyślnym szablon to klucz w klamrach
        ConstraintViolation<NullEmptyBlank> notNull = validator.validateValue(NullEmptyBlank.class, "notNull", null)
                .iterator().next();
        assertThat(notNull.getMessageTemplate()).isEqualTo("{jakarta.validation.constraints.NotNull.message}");
    }

    @Test
    @DisplayName("6. Komunikaty: własny tekst, {min}/{max}, klucz z ValidationMessages.properties (UTF-8), język domyślnych")
    void section6_messages() {
        ProductForm form = new ProductForm(" ", "ELE-001", null, 5000, "ab");
        Map<String, String> messages = new TreeMap<>();
        validator.validate(form).forEach(v -> messages.put(v.getPropertyPath().toString(), v.getMessage()));

        assertThat(messages.get("name")).isEqualTo("Nazwa produktu jest wymagana");                   // własny tekst
        assertThat(messages.get("description")).isEqualTo("Opis: od 3 do 20 znaków");                  // interpolacja {min} i {max}
        assertThat(messages.get("price")).isEqualTo("Cena produktu jest wymagana (bez ceny nie ma sprzedaży)");   // z pliku, polskie litery

        // wartość {value} z @DecimalMin
        assertThat(validator.validateValue(ProductForm.class, "price", new BigDecimal("0.001")).iterator().next().getMessage())
                .isEqualTo("Cena musi wynosić co najmniej 0.01 zł");

        // komunikat DOMYŚLNY zależy od języka — ustawiamy go jawnie (LocaleContextHolder: ustawienia dla bieżącego wątku)
        try {
            LocaleContextHolder.setLocale(Locale.ENGLISH);
            assertThat(validator.validateValue(ProductForm.class, "stock", 5000).iterator().next().getMessage())
                    .isEqualTo("must be less than or equal to 1000");
            LocaleContextHolder.setLocale(Locale.forLanguageTag("pl"));
            assertThat(validator.validateValue(ProductForm.class, "stock", 5000).iterator().next().getMessage())
                    .isEqualTo("musi być równe lub mniejsze od 1000");
        } finally {
            LocaleContextHolder.resetLocaleContext();   // sprzątanie — inne testy w tym wątku nie mogą tego odziedziczyć
        }
    }

    @Test
    @DisplayName("7. Bean Validation działa też bez Springa: Validation.buildDefaultValidatorFactory()")
    void section7_withoutSpring() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator plain = factory.getValidator();

            assertThat(plain).isNotInstanceOf(LocalValidatorFactoryBean.class);
            assertThat(paths(plain.validate(new ProductForm("", null, BigDecimal.ONE, 1, null))))
                    .containsExactly("name");
        }
    }
}
