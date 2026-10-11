package s03_boot_basics.solutions.boot01_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import s03_boot_basics.boot01_auto_configuration.Boot01Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Boot01AutoConfigurationExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma własnej klasy startowej, więc wskazujemy ją wprost: {@code classes = Boot01Application.class}.
 */
@Tag("wzorzec")
@SpringBootTest(classes = Boot01Application.class)
class Boot01AutoConfigurationSolutionsTest {

    @Autowired
    ConfigurableApplicationContext context;

    @Test
    @DisplayName("Wzorzec 1: nazwy beanów typu Executor")
    void solution1() {
        assertThat(Boot01AutoConfigurationSolutions.solution1(context)).containsExactly("applicationTaskExecutor");
    }

    @Test
    @DisplayName("Wzorzec 2: spełnione autokonfiguracje najwyższego poziomu")
    void solution2() {
        assertThat(Boot01AutoConfigurationSolutions.solution2(ConditionEvaluationReport.get(context.getBeanFactory())))
                .containsExactly("AopAutoConfiguration", "TaskExecutionAutoConfiguration", "TaskSchedulingAutoConfiguration");
    }

    @Test
    @DisplayName("Wzorzec 3: JMX włączony, własny executor zamiast applicationTaskExecutor")
    void solution3() {
        try (ConfigurableApplicationContext ctx = Boot01AutoConfigurationSolutions.solution3()) {
            assertThat(ctx.containsBean("mbeanExporter")).isTrue();
            assertThat(ctx.containsBean("shopExecutor")).isTrue();
            assertThat(ctx.containsBean("applicationTaskExecutor")).isFalse();
        }
    }
}
