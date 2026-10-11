package s03_boot_basics.boot01_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Boot01AutoConfiguration. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
class Boot01AutoConfigurationExercisesTest {

    @Autowired
    ConfigurableApplicationContext context;

    @Test
    @DisplayName("Ćwiczenie 1: nazwy beanów typu Executor")
    void exercise1() {
        assertThat(Boot01AutoConfiguration.exercise1(context)).containsExactly("applicationTaskExecutor");
    }

    @Test
    @DisplayName("Ćwiczenie 2: spełnione autokonfiguracje najwyższego poziomu")
    void exercise2() {
        assertThat(Boot01AutoConfiguration.exercise2(ConditionEvaluationReport.get(context.getBeanFactory())))
                .containsExactly("AopAutoConfiguration", "TaskExecutionAutoConfiguration", "TaskSchedulingAutoConfiguration");
    }

    @Test
    @DisplayName("Ćwiczenie 3: JMX włączony, własny executor zamiast applicationTaskExecutor")
    void exercise3() {
        try (ConfigurableApplicationContext ctx = Boot01AutoConfiguration.exercise3()) {
            assertThat(ctx.containsBean("mbeanExporter")).isTrue();
            assertThat(ctx.containsBean("shopExecutor")).isTrue();
            assertThat(ctx.containsBean("applicationTaskExecutor")).isFalse();
        }
    }
}
