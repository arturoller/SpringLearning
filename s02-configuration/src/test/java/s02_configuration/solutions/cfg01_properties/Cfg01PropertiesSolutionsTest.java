package s02_configuration.solutions.cfg01_properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Cfg01PropertiesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Nie potrzeba pełnego kontekstu Boota: ApplicationContextRunner wystarcza.
 */
@Tag("wzorzec")
class Cfg01PropertiesSolutionsTest {

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withInitializer(context ->
                context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance()));
    }

    @Test
    @DisplayName("Wzorzec 1: Greeter dostaje nazwę sklepu z cfg01.shop.name")
    void solution1() {
        runner().withUserConfiguration(Cfg01PropertiesSolutions.Greeter.class)
                .withPropertyValues("cfg01.shop.name=Kawiarnia")
                .run(context -> assertThat(context.getBean(Cfg01PropertiesSolutions.Greeter.class).greet())
                        .isEqualTo("Witaj w Kawiarnia!"));
    }

    @Test
    @DisplayName("Wzorzec 2: Limits ma wartości domyślne i da się je nadpisać")
    void solution2() {
        runner().withUserConfiguration(Cfg01PropertiesSolutions.Limits.class).run(context -> {
            Cfg01PropertiesSolutions.Limits limits = context.getBean(Cfg01PropertiesSolutions.Limits.class);
            assertThat(limits.retries()).isEqualTo(3);
            assertThat(limits.timeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(limits.enabled()).isTrue();
        });
        runner().withUserConfiguration(Cfg01PropertiesSolutions.Limits.class)
                .withPropertyValues("cfg01.exercise.retries=5", "cfg01.exercise.timeout=2m", "cfg01.exercise.enabled=false")
                .run(context -> {
                    Cfg01PropertiesSolutions.Limits limits = context.getBean(Cfg01PropertiesSolutions.Limits.class);
                    assertThat(limits.retries()).isEqualTo(5);
                    assertThat(limits.timeout()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(limits.enabled()).isFalse();
                });
    }

    @Test
    @DisplayName("Wzorzec 3: Counter dostaje limit przez konstruktor i liczy podwójną wartość")
    void solution3() {
        runner().withUserConfiguration(Cfg01PropertiesSolutions.Counter.class)
                .withPropertyValues("cfg01.exercise.limit=21")
                .run(context -> assertThat(context.getBean(Cfg01PropertiesSolutions.Counter.class).doubled).isEqualTo(42));
    }
}
