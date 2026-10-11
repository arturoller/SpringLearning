package s01_core_ioc.ioc04_bean_methods;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Ioc04BeanMethods. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Ioc04BeanMethodsExercisesTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-11T10:00:00Z"), ZoneId.of("UTC"));

    @Test
    @DisplayName("Ćwiczenie 1: prefiks faktury z zegara")
    void exercise1() {
        assertThat(Ioc04BeanMethods.exercise1(FIXED)).isEqualTo("FV/2026/10");
    }

    @Test
    @DisplayName("Ćwiczenie 2: bean Clock ze strefą Europe/Warsaw")
    void exercise2() {
        new ApplicationContextRunner().withUserConfiguration(Ioc04Exercise2Config.class).run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Europe/Warsaw"));
        });
    }

    @Test
    @DisplayName("Ćwiczenie 3: generator faktur zbudowany z beana Clock")
    void exercise3() {
        new ApplicationContextRunner()
                .withBean(Clock.class, () -> FIXED)
                .withUserConfiguration(Ioc04Exercise3Config.class)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(InvoiceNumberGenerator.class).next()).isEqualTo("FV/2026/10/0001");
                });
    }
}
