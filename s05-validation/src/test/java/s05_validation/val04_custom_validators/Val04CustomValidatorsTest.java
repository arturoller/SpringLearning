package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidationException;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Val04CustomValidators. {@code @WebMvcTest} + {@code @Import(CategoryCatalog.class)}: wycinek webowy
 * nie skanuje zwykłych beanów ({@code @Component}), więc słownik kategorii, którego potrzebuje walidator,
 * dorzucamy ręcznie. Walidator pochodzi z autokonfiguracji Boota (LocalValidatorFactoryBean).
 */
@WebMvcTest
@Import(CategoryCatalog.class)
class Val04CustomValidatorsTest {

    @Autowired
    Validator validator;

    @Autowired
    MockMvc mockMvc;

    private static Set<String> paths(Set<? extends ConstraintViolation<?>> violations) {
        Set<String> result = new TreeSet<>();
        violations.forEach(v -> result.add(v.getPropertyPath().toString()));
        return result;
    }

    @Test
    @DisplayName("1. @Sku: poprawny format przechodzi, zły nie, null przechodzi (to sprawa @NotNull)")
    void section1_customAnnotation() {
        assertThat(validator.validateValue(NewProduct.class, "sku", "ELE-001")).isEmpty();
        assertThat(validator.validateValue(NewProduct.class, "sku", "ele-001")).hasSize(1);
        assertThat(validator.validateValue(NewProduct.class, "sku", "ELE-1")).hasSize(1);
        assertThat(validator.validateValue(NewProduct.class, "sku", "ELE-0012")).hasSize(1);   // cały napis musi pasować

        // pole sku ma też @NotNull — dlatego null i tak jest błędem (ale nie z winy @Sku)
        ConstraintViolation<NewProduct> violation = validator.validateValue(NewProduct.class, "sku", null)
                .iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(jakarta.validation.constraints.NotNull.class);

        ConstraintViolation<NewProduct> bad = validator.validateValue(NewProduct.class, "sku", "zle").iterator().next();
        assertThat(bad.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Sku.class);
        assertThat(bad.getMessage()).isEqualTo("SKU ma format ELE-001 (trzy wielkie litery, myślnik, trzy cyfry)");
    }


    @Test
    @DisplayName("1b. Pułapki definicji: adnotacja bez atrybutu groups jest odrzucana; walidator zwracający false dla null robi z pola wymagane")
    void section1b_definitionTraps() {
        try (ValidatorFactory plainFactory = Validation.buildDefaultValidatorFactory()) {
            Validator plain = plainFactory.getValidator();

            assertThatThrownBy(() -> plain.validate(new BrokenForm("x")))
                    .isInstanceOf(jakarta.validation.ConstraintDefinitionException.class)
                    .hasMessageContaining("groups");

            assertThat(plain.validate(new NullHostileForm(null))).as("null odrzucony przez walidator").hasSize(1);
            assertThat(plain.validate(new NullHostileForm("ELE-001"))).isEmpty();
        }
    }
    @Test
    @DisplayName("2. Ograniczenie złożone @ProductCode: bez własnego walidatora; @ReportAsSingleViolation daje własny komunikat")
    void section2_composedConstraint() {
        assertThat(validator.validateValue(NewProduct.class, "altCode", "ELE-002")).isEmpty();
        assertThat(validator.validateValue(NewProduct.class, "altCode", null)).isEmpty();

        ConstraintViolation<NewProduct> violation = validator.validateValue(NewProduct.class, "altCode", "zle")
                .iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Kod produktu ma format ELE-001");
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(ProductCode.class);

        // to samo ograniczenie złożone, ale BEZ @ReportAsSingleViolation: zgłasza się składnik (@Pattern), z jego komunikatem
        record Raw(@RawCode String code) {
        }
        ConstraintViolation<Raw> raw = validator.validate(new Raw("zle")).iterator().next();
        assertThat(raw.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Pattern.class);
        assertThat(raw.getMessage()).isNotEqualTo("Własny komunikat, którego nikt nie zobaczy");
    }

    @Test
    @DisplayName("3. Ograniczenie na poziomie klasy: zakres dat — błąd przypięty do pola 'to' albo do całego obiektu")
    void section3_classLevel() throws Exception {
        LocalDate november1 = LocalDate.of(2026, 11, 1);
        LocalDate november30 = LocalDate.of(2026, 11, 30);

        assertThat(validator.validate(new PromotionRequest("Black Friday", november1, november30))).isEmpty();
        assertThat(validator.validate(new PromotionRequest("Jeden dzień", november1, november1))).isEmpty();
        assertThat(validator.validate(new PromotionRequest("Brak dat", null, null)).stream()
                .map(v -> v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()).toList())
                .as("tylko @NotNull z pól; reguła zakresu dla null milczy")
                .containsOnly("NotNull");

        Set<ConstraintViolation<PromotionRequest>> reversed =
                validator.validate(new PromotionRequest("Odwrotnie", november30, november1));
        assertThat(paths(reversed)).containsExactly("to");   // reportOnField = true (domyślnie)
        assertThat(reversed.iterator().next().getMessage()).isEqualTo("Data początkowa nie może być późniejsza niż końcowa");

        Set<ConstraintViolation<FlashSale>> flash = validator.validate(new FlashSale("Odwrotnie", november30, november1));
        assertThat(paths(flash)).containsExactly("");        // reportOnField = false: ścieżka pusta = błąd całego obiektu

        // w kontrolerze: błąd pola → FieldError, błąd obiektu → globalny (ObjectError)
        String json = "{\"name\":\"Odwrotnie\",\"from\":\"2026-11-30\",\"to\":\"2026-11-01\"}";
        MethodArgumentNotValidException onField = (MethodArgumentNotValidException) mockMvc
                .perform(post("/api/promotions").contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResolvedException();
        assertThat(onField.getBindingResult().getFieldErrors()).extracting(FieldError::getField).containsExactly("to");
        assertThat(onField.getBindingResult().getGlobalErrors()).isEmpty();

        MethodArgumentNotValidException onObject = (MethodArgumentNotValidException) mockMvc
                .perform(post("/api/flash-sales").contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResolvedException();
        assertThat(onObject.getBindingResult().getFieldErrors()).isEmpty();
        assertThat(onObject.getBindingResult().getGlobalErrors()).hasSize(1);
    }

    @Test
    @DisplayName("4. Walidator z zależnością (CategoryCatalog): działa z walidatorem Boota, a bez Springa zawodzi")
    void section4_springBeanInValidator() {
        assertThat(validator).isInstanceOf(LocalValidatorFactoryBean.class);

        assertThat(validator.validateValue(NewProduct.class, "category", "DOM")).isEmpty();
        assertThat(validator.validateValue(NewProduct.class, "category", "ZABAWKI")).hasSize(1);

        // czysta fabryka Bean Validation (bez Springa) nie umie podać CategoryCatalog do konstruktora
        try (ValidatorFactory plainFactory = Validation.buildDefaultValidatorFactory()) {
            Validator plain = plainFactory.getValidator();
            assertThatThrownBy(() -> plain.validateValue(NewProduct.class, "category", "DOM"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("HV000064")
                    .hasMessageContaining("Unable to instantiate ConstraintValidator")
                    .hasMessageContaining("KnownCategoryValidator");
        }
    }

    @Test
    @DisplayName("5. W kontrolerze własne ograniczenia działają przez zwykłe @Valid")
    void section5_inController() throws Exception {
        String good = "{\"name\":\"Kawa ziarnista 1kg\",\"sku\":\"SPO-001\",\"category\":\"SPOZYWCZE\"}";
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(good))
                .andExpect(status().isCreated());

        String bad = "{\"name\":\"Kawa\",\"sku\":\"zle\",\"category\":\"ZABAWKI\",\"altCode\":\"zle\"}";
        MvcResult result = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        MethodArgumentNotValidException ex = (MethodArgumentNotValidException) result.getResolvedException();
        assertThat(ex.getBindingResult().getFieldErrors().stream().map(FieldError::getField).sorted().toList())
                .containsExactly("altCode", "category", "sku");
    }

    @Test
    @DisplayName("6. Wartość od użytkownika w szablonie komunikatu: {klucz} jest podmieniany; addMessageParameter jest bezpieczny")
    void section6_messageTemplateInjection() {
        String sneaky = "{jakarta.validation.constraints.NotNull.message}";
        try (ValidatorFactory plainFactory = Validation.buildDefaultValidatorFactory()) {
            Validator plain = plainFactory.getValidator();

            // ŹLE: wartość sklejona z szablonem — Bean Validation „wykonuje” zawarte w niej {klucze}
            String leaky = plain.validate(new LeakyForm(sneaky)).iterator().next().getMessage();
            assertThat(leaky).doesNotContain(sneaky).startsWith("Zła wartość: ");

            // DOBRZE: wartość jako parametr — wstawiana dosłownie
            String safe = plain.validate(new SafeForm(sneaky)).iterator().next().getMessage();
            assertThat(safe).isEqualTo("Zła wartość: " + sneaky);

            // wyrażenia ${...} w takim szablonie Hibernate Validator 9 domyślnie nie wykonuje
            assertThat(plain.validate(new LeakyForm("${1+1}")).iterator().next().getMessage())
                    .isEqualTo("Zła wartość: ${1+1}");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Pomocnicze ograniczenia TYLKO do testu (sekcje 2 i 6). Klasy walidatorów muszą być publiczne — Bean Validation
    // tworzy je przez refleksję.
    // ---------------------------------------------------------------------------------------------

    /** Ograniczenie złożone BEZ @ReportAsSingleViolation. */
    @Pattern(regexp = "[A-Z]{3}-[0-9]{3}")
    @Documented
    @Constraint(validatedBy = {})
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RawCode {
        String message() default "Własny komunikat, którego nikt nie zobaczy";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    @Documented
    @Constraint(validatedBy = LeakyValidator.class)
    @Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Leaky {
        String message() default "x";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    @Documented
    @Constraint(validatedBy = SafeValidator.class)
    @Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Safe {
        String message() default "x";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    /** ŹLE: skleja wartość od użytkownika z szablonem komunikatu. */
    public static class LeakyValidator implements ConstraintValidator<Leaky, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Zła wartość: " + value).addConstraintViolation();
            return false;
        }
    }

    /** DOBRZE: wartość trafia do komunikatu jako parametr {value}, a nie jako część szablonu. */
    public static class SafeValidator implements ConstraintValidator<Safe, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            context.unwrap(HibernateConstraintValidatorContext.class)
                    .addMessageParameter("value", value)
                    .buildConstraintViolationWithTemplate("Zła wartość: {value}")
                    .addConstraintViolation();
            return false;
        }
    }

    /** BŁĄD w definicji: brak obowiązkowego atrybutu groups(). */
    @Documented
    @Constraint(validatedBy = {})
    @Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Broken {
        String message() default "x";

        Class<? extends Payload>[] payload() default {};
    }

    @Documented
    @Constraint(validatedBy = NullHostileValidator.class)
    @Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NullHostile {
        String message() default "x";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    /** Walidator „wrogi wobec null”: null uznaje za błąd (przeciwnie do zasady z lekcji). */
    public static class NullHostileValidator implements ConstraintValidator<NullHostile, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value != null && value.matches("[A-Z]{3}-[0-9]{3}");
        }
    }

    public record BrokenForm(@Broken String text) {
    }

    public record NullHostileForm(@NullHostile String sku) {
    }

    public record LeakyForm(@Leaky String text) {
    }

    public record SafeForm(@Safe String text) {
    }
}
