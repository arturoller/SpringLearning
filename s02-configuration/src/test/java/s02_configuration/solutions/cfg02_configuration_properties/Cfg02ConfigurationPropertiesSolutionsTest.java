package s02_configuration.solutions.cfg02_configuration_properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.unit.DataSize;
import s02_configuration.cfg02_configuration_properties.DeliveryProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprawdzenia z Cfg02ConfigurationPropertiesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Lekki kontekst ({@code ApplicationContextRunner}) wystarcza, więc nie potrzeba klasy startowej.
 */
@Tag("wzorzec")
class Cfg02ConfigurationPropertiesSolutionsTest {

    @EnableConfigurationProperties(Cfg02ConfigurationPropertiesSolutions.Discount.class)
    static class EnableDiscount {
    }

    @EnableConfigurationProperties(DeliveryProperties.class)
    static class EnableDelivery {
    }

    @Test
    @DisplayName("Wzorzec 1: Discount ma prefiks cfg02.exercise.discount i wartości domyślne 10 / true")
    void solution1() {
        new ApplicationContextRunner().withUserConfiguration(EnableDiscount.class).run(context -> {
            Cfg02ConfigurationPropertiesSolutions.Discount discount =
                    context.getBean(Cfg02ConfigurationPropertiesSolutions.Discount.class);
            assertThat(discount.percent()).isEqualTo(10);
            assertThat(discount.enabled()).isTrue();
        });
        new ApplicationContextRunner().withUserConfiguration(EnableDiscount.class)
                .withPropertyValues("cfg02.exercise.discount.percent=25", "cfg02.exercise.discount.enabled=false")
                .run(context -> {
                    Cfg02ConfigurationPropertiesSolutions.Discount discount =
                            context.getBean(Cfg02ConfigurationPropertiesSolutions.Discount.class);
                    assertThat(discount.percent()).isEqualTo(25);
                    assertThat(discount.enabled()).isFalse();
                });
    }

    @Test
    @DisplayName("Wzorzec 2: ustawienia tekstowe wiążą się z DeliveryProperties")
    void solution2() {
        new ApplicationContextRunner().withUserConfiguration(EnableDelivery.class)
                .withPropertyValues(Cfg02ConfigurationPropertiesSolutions.solution2())
                .run(context -> {
                    DeliveryProperties properties = context.getBean(DeliveryProperties.class);
                    assertThat(properties.carrier()).isEqualTo("DPD");
                    assertThat(properties.maxParcels()).isEqualTo(2);
                    assertThat(properties.cities()).containsExactly("Poznań", "Wrocław");
                    assertThat(properties.prices()).containsOnlyKeys("poznan", "wroclaw");
                    assertThat(properties.prices().get("wroclaw")).isEqualByComparingTo("13.50");
                });
    }

    @Test
    @DisplayName("Wzorzec 3: koszt wysyłki z cennika i limitem paczek")
    void solution3() {
        DeliveryProperties properties = new DeliveryProperties("DPD", 2, Duration.ofSeconds(5), DataSize.ofBytes(1),
                new DeliveryProperties.Warehouse("Poznań", null), List.of("Poznań"),
                Map.of("poznan", new BigDecimal("11.00")));

        assertThat(Cfg02ConfigurationPropertiesSolutions.solution3(properties, "poznan", 2)).isEqualByComparingTo("22.00");
        assertThatThrownBy(() -> Cfg02ConfigurationPropertiesSolutions.solution3(properties, "poznan", 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Cfg02ConfigurationPropertiesSolutions.solution3(properties, "gdynia", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
