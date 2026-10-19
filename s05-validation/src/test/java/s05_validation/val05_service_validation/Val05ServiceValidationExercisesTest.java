package s05_validation.val05_service_validation;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy ĆWICZEŃ lekcji Val05ServiceValidation. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Walidacja metod wymaga proxy, więc używamy kontekstu Springa ({@code @SpringBootTest}).
 */
@Tag("cwiczenie")
@SpringBootTest
class Val05ServiceValidationExercisesTest {

    @Autowired
    DiscountService discountService;

    @Autowired
    GreetingService greetingService;

    @Autowired
    ProductService productService;

    @Test
    @DisplayName("Ćwiczenie 1: DiscountService waliduje cenę i procent")
    void exercise1() {
        assertThat(discountService.apply(new BigDecimal("200.00"), 25)).isEqualByComparingTo("150.00");
        assertThat(discountService.apply(new BigDecimal("99.99"), 0)).isEqualByComparingTo("99.99");

        assertThatThrownBy(() -> discountService.apply(new BigDecimal("-1"), 10)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> discountService.apply(null, 10)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> discountService.apply(BigDecimal.TEN, 101)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> discountService.apply(BigDecimal.TEN, -1)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 2: greet(\" \") z zewnątrz rzuca ConstraintViolationException")
    void exercise2() {
        assertThat(greetingService.greet("  Jan ")).isEqualTo("Witaj, Jan!");
        assertThatThrownBy(() -> greetingService.greet(" ")).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> greetingService.greet(null)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 3: wyjątek walidacji → mapa pole → komunikat (ostatni element ścieżki)")
    void exercise3() {
        ConstraintViolationException exception = null;
        try {
            productService.add(new NewProduct(" ", "x", BigDecimal.ONE));
        } catch (ConstraintViolationException e) {
            exception = e;
        }
        assertThat(exception).isNotNull();

        Map<String, String> byField = Val05ServiceValidation.violationsByField(exception);

        assertThat(byField).containsOnlyKeys("name", "sku");   // posortowana mapa: name, sku
        assertThat(byField).containsEntry("name", "Nazwa jest wymagana").containsEntry("sku", "SKU ma format np. ELE-001");
        assertThat(byField.keySet()).containsExactly("name", "sku");
    }
}
