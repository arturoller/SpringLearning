package s05_validation.solutions.val05_service_validation;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import s05_validation.val05_service_validation.NewProduct;
import s05_validation.val05_service_validation.ProductService;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy rozwiązań wzorcowych ćwiczeń Val05ServiceValidation — działają domyślnie (dowód, że ćwiczenia da się rozwiązać).
 * Własna konfiguracja: {@link Val05SolutionsApplication} (pakiet rozwiązań to rodzeństwo pakietu lekcji).
 */
@Tag("wzorzec")
@SpringBootTest(classes = {Val05SolutionsApplication.class, ProductService.class})   // ProductService z lekcji — do ćwiczenia 3
class Val05ServiceValidationSolutionsTest {

    @Autowired
    SolutionDiscountService discountService;

    @Autowired
    SolutionGreetingService greetingService;

    @Autowired
    ProductService productService;

    @Test
    @DisplayName("Rozwiązanie 1: SolutionDiscountService waliduje cenę i procent")
    void solution1() {
        assertThat(discountService.apply(new BigDecimal("200.00"), 25)).isEqualByComparingTo("150.00");
        assertThat(discountService.apply(new BigDecimal("99.99"), 0)).isEqualByComparingTo("99.99");

        assertThatThrownBy(() -> discountService.apply(new BigDecimal("-1"), 10)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> discountService.apply(null, 10)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> discountService.apply(BigDecimal.TEN, 101)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> discountService.apply(BigDecimal.TEN, -1)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Rozwiązanie 2: greet(\" \") z zewnątrz rzuca ConstraintViolationException")
    void solution2() {
        assertThat(greetingService.greet("  Jan ")).isEqualTo("Witaj, Jan!");
        assertThatThrownBy(() -> greetingService.greet(" ")).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> greetingService.greet(null)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Rozwiązanie 3: wyjątek walidacji → mapa pole → komunikat (ostatni element ścieżki)")
    void solution3() {
        ConstraintViolationException exception = null;
        try {
            productService.add(new NewProduct(" ", "x", BigDecimal.ONE));
        } catch (ConstraintViolationException e) {
            exception = e;
        }
        assertThat(exception).isNotNull();

        Map<String, String> byField = Val05ServiceValidationSolutions.violationsByField(exception);

        assertThat(byField).containsEntry("name", "Nazwa jest wymagana").containsEntry("sku", "SKU ma format np. ELE-001");
        assertThat(byField.keySet()).containsExactly("name", "sku");
    }
}
