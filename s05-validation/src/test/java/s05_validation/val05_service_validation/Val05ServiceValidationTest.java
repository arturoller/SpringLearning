package s05_validation.val05_service_validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Val05ServiceValidation (sekcje 1–3). {@code @SpringBootTest}, bo proxy z walidacją tworzy kontener
 * Springa — bez niego (zwykłe {@code new}) nie ma żadnej walidacji, i właśnie to pokazuje sekcja 3.
 */
@SpringBootTest(classes = Val05Application.class)
class Val05ServiceValidationTest {

    @Autowired
    StockService stockService;

    @Autowired
    BatchService batchService;

    @Autowired
    ApplicationContext context;

    @Autowired
    Validator validator;

    private static List<String> violations(ConstraintViolationException e) {
        return e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " (" + v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName() + ")")
                .sorted().toList();
    }

    // ------------------------------------------------------------------------------------------ sekcja 1

    @Test
    @DisplayName("Sekcja 1: bean z @Validated jest proxy (CGLIB) tworzonym przez autokonfigurowany MethodValidationPostProcessor")
    void validatedBeanIsProxy() {
        assertThat(context.getBeanNamesForType(MethodValidationPostProcessor.class)).containsExactly("methodValidationPostProcessor");
        assertThat(AopUtils.isAopProxy(stockService)).isTrue();
        assertThat(AopUtils.isCglibProxy(stockService)).as("domyślnie proxy CGLIB — podklasa serwisu").isTrue();
        assertThat(stockService.getClass()).isNotEqualTo(StockService.class).hasSuperclass(StockService.class);
    }

    @Test
    @DisplayName("Sekcja 1: zły parametr = ConstraintViolationException, a metoda się NIE wykonuje; ścieżka to metoda.parametr")
    void invalidParameters() {
        int before = stockService.log().size();

        assertThatThrownBy(() -> stockService.reserve("", -1))
                .isInstanceOf(ConstraintViolationException.class)
                .satisfies(e -> assertThat(violations((ConstraintViolationException) e))
                        .containsExactly("reserve.quantity (Positive)", "reserve.sku (NotBlank)"));

        assertThat(stockService.log()).as("ciało metody nie zostało wykonane").hasSize(before);
        assertThat(stockService.reserve("KAW-0001", 3)).isEqualTo(3);
    }

    @Test
    @DisplayName("Sekcja 1: @Valid na parametrze wchodzi do obiektu (register.item.sku), a null łamie @NotNull")
    void cascadeAndNull() {
        assertThatThrownBy(() -> stockService.register(new NewItem(" ", 0)))
                .isInstanceOf(ConstraintViolationException.class)
                .satisfies(e -> assertThat(violations((ConstraintViolationException) e))
                        .containsExactly("register.item.quantity (Positive)", "register.item.sku (NotBlank)"));

        assertThatThrownBy(() -> stockService.register(null))
                .isInstanceOf(ConstraintViolationException.class)
                .satisfies(e -> assertThat(violations((ConstraintViolationException) e)).containsExactly("register.item (NotNull)"));

        assertThatCode(() -> stockService.register(new NewItem("KAW-0001", 1))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Sekcja 1: bez @Valid na parametrze adnotacje wewnątrz obiektu są pomijane")
    void noCascadeWithoutValid() {
        assertThatCode(() -> stockService.registerWithoutCascade(new NewItem(" ", 0))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Sekcja 1: ograniczenie na wyniku metody łapie błąd w samym serwisie (ścieżka label.<return value>)")
    void returnValueIsValidated() {
        assertThat(stockService.label("KAW-0001", false)).isEqualTo("SKU: KAW-0001");

        assertThatThrownBy(() -> stockService.label("KAW-0001", true))
                .isInstanceOf(ConstraintViolationException.class)
                .satisfies(e -> assertThat(violations((ConstraintViolationException) e))
                        .containsExactly("label.<return value> (NotBlank)"));
    }

    // ------------------------------------------------------------------------------------------ sekcja 2

    @Test
    @DisplayName("Sekcja 2: wywołanie własnej metody (this) omija proxy — złe dane przechodzą bez wyjątku")
    void selfInvocationBypassesProxy() {
        int before = stockService.log().size();

        assertThatCode(() -> stockService.reserveTwiceBroken("", -5)).doesNotThrowAnyException();

        assertThat(stockService.log()).as("reserve wykonał się dwa razy z błędnymi danymi")
                .hasSize(before + 2).endsWith("reserve  x-5");
    }

    @Test
    @DisplayName("Sekcja 2: to samo wywołanie przez INNY bean przechodzi przez proxy i jest walidowane")
    void callThroughAnotherBeanIsValidated() {
        int before = stockService.log().size();

        assertThatThrownBy(() -> batchService.reserveTwice("", -5)).isInstanceOf(ConstraintViolationException.class);

        assertThat(stockService.log()).hasSize(before);
    }

    // ------------------------------------------------------------------------------------------ sekcja 3

    @Test
    @DisplayName("Sekcja 3: obiekt i serwis utworzone przez new nie są walidowane — trzeba wywołać Validator")
    void newBypassesValidation() {
        NewItem bad = new NewItem("", -1);                 // żaden wyjątek: adnotacje nie działają same
        StockService notManaged = new StockService();      // goły obiekt, bez proxy

        assertThatCode(() -> notManaged.reserve("", -1)).doesNotThrowAnyException();

        Set<ConstraintViolation<NewItem>> violations = validator.validate(bad);   // dopiero ręczne sprawdzenie
        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactlyInAnyOrder("sku", "quantity");
    }
}
