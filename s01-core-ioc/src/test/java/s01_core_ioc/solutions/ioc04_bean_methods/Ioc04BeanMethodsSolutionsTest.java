package s01_core_ioc.solutions.ioc04_bean_methods;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import s01_core_ioc.ioc04_bean_methods.InvoiceNumberGenerator;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Ioc04BeanMethodsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Ioc04BeanMethodsSolutionsTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-11T10:00:00Z"), ZoneId.of("UTC"));

    @Test
    @DisplayName("Wzorzec 1: prefiks faktury z zegara")
    void solution1() {
        assertThat(Ioc04BeanMethodsSolutions.solution1(FIXED)).isEqualTo("FV/2026/10");
    }

    @Test
    @DisplayName("Wzorzec 2: bean Clock ze strefą Europe/Warsaw")
    void solution2() {
        new ApplicationContextRunner().withUserConfiguration(Ioc04Exercise2ConfigSolution.class).run(ctx ->
                assertThat(ctx.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Europe/Warsaw")));
    }

    @Test
    @DisplayName("Wzorzec 3: generator faktur zbudowany z beana Clock")
    void solution3() {
        new ApplicationContextRunner()
                .withBean(Clock.class, () -> FIXED)
                .withUserConfiguration(Ioc04Exercise3ConfigSolution.class)
                .run(ctx -> assertThat(ctx.getBean(InvoiceNumberGenerator.class).next()).isEqualTo("FV/2026/10/0001"));
    }
}
