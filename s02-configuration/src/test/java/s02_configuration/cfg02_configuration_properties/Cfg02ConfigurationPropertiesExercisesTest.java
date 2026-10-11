package s02_configuration.cfg02_configuration_properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.unit.DataSize;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Cfg02ConfigurationProperties. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Cfg02ConfigurationPropertiesExercisesTest {

    @EnableConfigurationProperties(Cfg02ConfigurationProperties.Discount.class)
    static class EnableDiscount {
    }

    @EnableConfigurationProperties(DeliveryProperties.class)
    static class EnableDelivery {
    }

    @Test
    @DisplayName("Ćwiczenie 1: Discount ma prefiks cfg02.exercise.discount i wartości domyślne 10 / true")
    void exercise1() {
        new ApplicationContextRunner().withUserConfiguration(EnableDiscount.class).run(context -> {
            Cfg02ConfigurationProperties.Discount discount = context.getBean(Cfg02ConfigurationProperties.Discount.class);
            assertThat(discount.percent()).isEqualTo(10);
            assertThat(discount.enabled()).isTrue();
        });
        new ApplicationContextRunner().withUserConfiguration(EnableDiscount.class)
                .withPropertyValues("cfg02.exercise.discount.percent=25", "cfg02.exercise.discount.enabled=false")
                .run(context -> {
                    Cfg02ConfigurationProperties.Discount discount = context.getBean(Cfg02ConfigurationProperties.Discount.class);
                    assertThat(discount.percent()).isEqualTo(25);
                    assertThat(discount.enabled()).isFalse();
                });
    }

    @Test
    @DisplayName("Ćwiczenie 2: ustawienia tekstowe wiążą się z DeliveryProperties")
    void exercise2() {
        new ApplicationContextRunner().withUserConfiguration(EnableDelivery.class)
                .withPropertyValues(Cfg02ConfigurationProperties.exercise2())
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
    @DisplayName("Ćwiczenie 3: koszt wysyłki z cennika i limitem paczek")
    void exercise3() {
        DeliveryProperties properties = new DeliveryProperties("DPD", 2, Duration.ofSeconds(5), DataSize.ofBytes(1),
                new DeliveryProperties.Warehouse("Poznań", null), List.of("Poznań"),
                Map.of("poznan", new BigDecimal("11.00")));

        assertThat(Cfg02ConfigurationProperties.exercise3(properties, "poznan", 2)).isEqualByComparingTo("22.00");
        assertThatThrownBy(() -> Cfg02ConfigurationProperties.exercise3(properties, "poznan", 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Cfg02ConfigurationProperties.exercise3(properties, "gdynia", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
