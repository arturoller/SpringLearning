package s05_validation.val05_service_validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy sekcji 1-3 lekcji Val05ServiceValidation: {@code @SpringBootTest} — proxy z walidacją powstaje dopiero
 * w prawdziwym kontekście Springa (zwykłe {@code new PriceService()} nie miałoby proxy, więc niczego by nie sprawdziło).
 */
@SpringBootTest
class Val05ServiceValidationTest {

    @Autowired
    PriceService priceService;

    @Autowired
    BatchPriceService batchPriceService;

    @Autowired
    ApplicationContext context;

    private static List<String> paths(ConstraintViolationException e) {
        return e.getConstraintViolations().stream()
                .map((ConstraintViolation<?> v) -> v.getPropertyPath().toString()).sorted().toList();
    }

    @Test
    @DisplayName("1. @Validated na serwisie: bean jest proxy, a zły parametr daje ConstraintViolationException zanim metoda ruszy")
    void methodValidationOnService() {
        assertThat(context.getBean(MethodValidationPostProcessor.class)).isNotNull();   // z autokonfiguracji Boota
        assertThat(AopUtils.isAopProxy(priceService)).isTrue();                         // wstrzyknięty jest PROXY
        assertThat(priceService.getClass().getName()).contains("SpringCGLIB");

        assertThat(priceService.gross(10_000, 23)).isEqualTo(12_300);

        assertThatThrownBy(() -> priceService.gross(100, 500))
                .isInstanceOfSatisfying(ConstraintViolationException.class,
                        e -> assertThat(paths(e)).containsExactly("gross.vatPercent"));
        assertThatThrownBy(() -> priceService.gross(-1, 500))
                .isInstanceOfSatisfying(ConstraintViolationException.class,
                        e -> assertThat(paths(e)).containsExactly("gross.netGrosze", "gross.vatPercent"));
    }

    @Test
    @DisplayName("2. Wynik metody i obiekt-parametr: <return value>, null łapie @NotNull, a @Valid schodzi do pól")
    void returnValueAndObjectParameter() {
        assertThatThrownBy(() -> priceService.faultyDiscount())
                .isInstanceOfSatisfying(ConstraintViolationException.class,
                        e -> assertThat(paths(e)).containsExactly("faultyDiscount.<return value>"));

        assertThat(priceService.describe(new ProductCommand("Mleko", 349))).isEqualTo("Mleko za 349 gr");
        assertThatThrownBy(() -> priceService.describe(null))
                .isInstanceOfSatisfying(ConstraintViolationException.class,
                        e -> assertThat(paths(e)).containsExactly("describe.command"));
        assertThatThrownBy(() -> priceService.describe(new ProductCommand(" ", 0)))
                .isInstanceOfSatisfying(ConstraintViolationException.class,
                        e -> assertThat(paths(e)).containsExactly("describe.command.name", "describe.command.priceGrosze"));
    }

    @Test
    @DisplayName("3. Self-invocation: this.gross(...) omija proxy (brak walidacji); przez inny bean walidacja działa")
    void selfInvocation() {
        // z zewnątrz — walidacja działa
        assertThatThrownBy(() -> priceService.gross(100, 500)).isInstanceOf(ConstraintViolationException.class);

        // ta sama zła stawka, ale wywołana przez this wewnątrz PriceService: 100 + 100 * 500 / 100 = 600, bez wyjątku
        assertThatCode(() -> priceService.grossOfAll(List.of(100), 500)).doesNotThrowAnyException();
        assertThat(priceService.grossOfAll(List.of(100), 500)).isEqualTo(600);

        // naprawa: logika w INNYM beanie woła serwis przez proxy
        assertThatThrownBy(() -> batchPriceService.grossOfAll(List.of(100), 500))
                .isInstanceOf(ConstraintViolationException.class);
        assertThat(batchPriceService.grossOfAll(List.of(100, 200), 23)).isEqualTo(369);
    }
}
