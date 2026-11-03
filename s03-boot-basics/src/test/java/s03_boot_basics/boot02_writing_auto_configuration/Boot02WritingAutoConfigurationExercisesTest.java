package s03_boot_basics.boot02_writing_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Boot02WritingAutoConfiguration. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Boot02WritingAutoConfigurationExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: ścieżka pliku imports wskazuje istniejący zasób z PriceAutoConfiguration")
    void exercise1() throws Exception {
        ClassPathResource resource = new ClassPathResource(Boot02WritingAutoConfiguration.exercise1ImportsFileLocation());
        assertThat(resource.exists()).isTrue();
        assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).contains(PriceAutoConfiguration.class.getName());
    }

    @Test
    @DisplayName("Ćwiczenie 2: kalkulator wysyłki tylko z przełącznikiem i z ustępowaniem przed beanem aplikacji")
    void exercise2() {
        ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(
                AutoConfigurations.of(Boot02WritingAutoConfiguration.Exercise2ShippingAutoConfiguration.class));
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(ShippingCostCalculator.class));
        runner.withPropertyValues("boot02.exercise.shipping.enabled=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(ShippingCostCalculator.class);
            assertThat(ctx.getBean(ShippingCostCalculator.class).freeFrom()).isEqualByComparingTo("200");
        });
        ShippingCostCalculator mine = new ShippingCostCalculator(new BigDecimal("50"));
        runner.withPropertyValues("boot02.exercise.shipping.enabled=true")
                .withBean("shopShipping", ShippingCostCalculator.class, () -> mine)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ShippingCostCalculator.class);
                    assertThat(ctx.getBean(ShippingCostCalculator.class)).isSameAs(mine);
                });
    }

    @Test
    @DisplayName("Ćwiczenie 3: próg darmowej wysyłki z ustawień, domyślnie 150")
    void exercise3() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Boot02WritingAutoConfiguration.Exercise3DeliveryAutoConfiguration.class));
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(ShippingCostCalculator.class));
        runner.withPropertyValues("boot02.exercise.delivery.enabled=true").run(ctx -> {
            ShippingCostCalculator calculator = ctx.getBean(ShippingCostCalculator.class);
            assertThat(calculator.freeFrom()).isEqualByComparingTo("150");
            assertThat(calculator.cost(new BigDecimal("149.99"))).isEqualByComparingTo("15");
        });
        runner.withPropertyValues("boot02.exercise.delivery.enabled=true", "boot02.exercise.delivery.free-from=99")
                .run(ctx -> assertThat(ctx.getBean(ShippingCostCalculator.class).cost(new BigDecimal("99")))
                        .isEqualByComparingTo("0"));
    }

    @Test
    @DisplayName("Ćwiczenie 4: etykiety powstają po formaterze niezależnie od kolejności podania klas")
    void exercise4() {
        ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                Boot02WritingAutoConfiguration.Exercise4PriceTagAutoConfiguration.class, PriceAutoConfiguration.class));
        runner.withPropertyValues("boot02.price.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(PriceTagPrinter.class));
        runner.run(ctx -> assertThat(ctx).hasNotFailed().doesNotHaveBean(PriceTagPrinter.class));
    }
}
