package s03_boot_basics.solutions.boot02_writing_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import s03_boot_basics.boot02_writing_auto_configuration.PriceAutoConfiguration;
import s03_boot_basics.boot02_writing_auto_configuration.PriceTagPrinter;
import s03_boot_basics.boot02_writing_auto_configuration.ShippingCostCalculator;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Boot02WritingAutoConfigurationExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną
 * zawsze. Tylko ApplicationContextRunner — bez klasy startowej i bez dopisywania czegokolwiek do pliku imports.
 */
@Tag("wzorzec")
class Boot02WritingAutoConfigurationSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: ścieżka pliku imports wskazuje istniejący zasób z PriceAutoConfiguration")
    void solution1() throws Exception {
        ClassPathResource resource = new ClassPathResource(Boot02WritingAutoConfigurationSolutions.solution1ImportsFileLocation());
        assertThat(resource.exists()).isTrue();
        assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).contains(PriceAutoConfiguration.class.getName());
    }

    @Test
    @DisplayName("Wzorzec 2: kalkulator wysyłki tylko z przełącznikiem i z ustępowaniem przed beanem aplikacji")
    void solution2() {
        ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(
                AutoConfigurations.of(Boot02WritingAutoConfigurationSolutions.ShippingAutoConfiguration.class));
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
    @DisplayName("Wzorzec 3: próg darmowej wysyłki z ustawień, domyślnie 150")
    void solution3() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Boot02WritingAutoConfigurationSolutions.DeliveryAutoConfiguration.class));
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
    @DisplayName("Wzorzec 4: etykiety powstają po formaterze niezależnie od kolejności podania klas")
    void solution4() {
        ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                Boot02WritingAutoConfigurationSolutions.PriceTagAutoConfiguration.class, PriceAutoConfiguration.class));
        runner.withPropertyValues("boot02.price.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(PriceTagPrinter.class));
        runner.run(ctx -> assertThat(ctx).hasNotFailed().doesNotHaveBean(PriceTagPrinter.class));
    }
}
