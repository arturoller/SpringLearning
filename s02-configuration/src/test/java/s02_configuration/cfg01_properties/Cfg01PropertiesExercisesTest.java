package s02_configuration.cfg01_properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Cfg01Properties. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Używamy ApplicationContextRunner (lekki kontekst bez skanowania) z konwersją typów Boota.
 */
@Tag("cwiczenie")
class Cfg01PropertiesExercisesTest {

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withInitializer(context ->
                context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance()));
    }

    @Test
    @DisplayName("Ćwiczenie 1: Greeter dostaje nazwę sklepu z cfg01.shop.name")
    void exercise1() {
        runner().withUserConfiguration(Cfg01Properties.Greeter.class)
                .withPropertyValues("cfg01.shop.name=Kawiarnia")
                .run(context -> assertThat(context.getBean(Cfg01Properties.Greeter.class).greet())
                        .isEqualTo("Witaj w Kawiarnia!"));
    }

    @Test
    @DisplayName("Ćwiczenie 2: Limits ma wartości domyślne i da się je nadpisać")
    void exercise2() {
        runner().withUserConfiguration(Cfg01Properties.Limits.class).run(context -> {
            Cfg01Properties.Limits limits = context.getBean(Cfg01Properties.Limits.class);
            assertThat(limits.retries()).isEqualTo(3);
            assertThat(limits.timeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(limits.enabled()).isTrue();
        });
        runner().withUserConfiguration(Cfg01Properties.Limits.class)
                .withPropertyValues("cfg01.exercise.retries=5", "cfg01.exercise.timeout=2m", "cfg01.exercise.enabled=false")
                .run(context -> {
                    Cfg01Properties.Limits limits = context.getBean(Cfg01Properties.Limits.class);
                    assertThat(limits.retries()).isEqualTo(5);
                    assertThat(limits.timeout()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(limits.enabled()).isFalse();
                });
    }

    @Test
    @DisplayName("Ćwiczenie 3: Counter dostaje limit przez konstruktor i liczy podwójną wartość")
    void exercise3() {
        runner().withUserConfiguration(Cfg01Properties.Counter.class)
                .withPropertyValues("cfg01.exercise.limit=21")
                .run(context -> assertThat(context.getBean(Cfg01Properties.Counter.class).doubled).isEqualTo(42));
    }
}
