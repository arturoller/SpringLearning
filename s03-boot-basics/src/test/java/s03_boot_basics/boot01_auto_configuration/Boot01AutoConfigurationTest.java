package s03_boot_basics.boot01_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Boot01AutoConfiguration.
 * <ul>
 *   <li>{@code @SpringBootTest} — pełny kontekst Boot01Application (sekcje 1 i 5: prawdziwa autokonfiguracja).</li>
 *   <li>ApplicationContextRunner (uruchamiacz kontekstu) — mały, szybki kontekst z WYBRANYMI autokonfiguracjami
 *       (sekcje 2–4). Każde run(...) tworzy nowy kontekst i sam go zamyka.</li>
 *   <li>SpringApplicationBuilder — sekcja 6, bo wykluczenia przez właściwość obsługuje dopiero
 *       {@code @EnableAutoConfiguration} (runner z AutoConfigurations.of ich nie czyta).</li>
 * </ul>
 */
@SpringBootTest
class Boot01AutoConfigurationTest {

    private static final String TASK_EXECUTION = "org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration";

    /** Runner z jedną autokonfiguracją: TaskExecutionAutoConfiguration. */
    private final ApplicationContextRunner taskRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class));

    @Autowired
    ConfigurableApplicationContext context;

    @Test
    @DisplayName("1. Pusta aplikacja ma bean applicationTaskExecutor — dołożyła go autokonfiguracja z pliku .imports")
    void autoConfigurationAddsBeans() {
        assertThat(AnnotatedElementUtils.hasAnnotation(Boot01Application.class, EnableAutoConfiguration.class))
                .as("@SpringBootApplication zawiera @EnableAutoConfiguration").isTrue();
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates())
                .contains(TASK_EXECUTION);
        assertThat(context.containsBean("applicationTaskExecutor")).isTrue();
        assertThat(context.getType("applicationTaskExecutor")).isEqualTo(ThreadPoolTaskExecutor.class);
    }

    @Test
    @DisplayName("2. @ConditionalOnClass: bez klasy ThreadPoolTaskExecutor autokonfiguracja się nie włącza")
    void conditionalOnClass() {
        taskRunner.run(ctx -> assertThat(ctx).hasBean("applicationTaskExecutor"));
        taskRunner.withClassLoader(new FilteredClassLoader(ThreadPoolTaskExecutor.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean("applicationTaskExecutor")
                        .doesNotHaveBean(TaskExecutionAutoConfiguration.class));
    }

    @Test
    @DisplayName("3. @ConditionalOnMissingBean: własny Executor wygrywa, applicationTaskExecutor znika (pytanie 2)")
    void conditionalOnMissingBeanBackOff() {
        taskRunner.withUserConfiguration(Boot01OwnExecutorConfig.class)
                .run(ctx -> assertThat(ctx)
                        .hasBean("shopExecutor")
                        .doesNotHaveBean("applicationTaskExecutor"));
    }

    @Test
    @DisplayName("4. @ConditionalOnBooleanProperty: JMX tylko po ustawieniu spring.jmx.enabled=true (pytanie 3)")
    void conditionalOnProperty() {
        ApplicationContextRunner jmxRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class));
        jmxRunner.run(ctx -> assertThat(ctx).doesNotHaveBean("mbeanExporter"));
        jmxRunner.withPropertyValues("spring.jmx.enabled=true")
                .run(ctx -> assertThat(ctx).hasBean("mbeanExporter"));
    }

    @Test
    @DisplayName("5. Raport warunków: TaskExecution spełniona, JMX i MessageSource niespełnione")
    void conditionEvaluationReport() {
        ConditionEvaluationReport report = ConditionEvaluationReport.get(context.getBeanFactory());
        Map<String, ConditionEvaluationReport.ConditionAndOutcomes> outcomes = report.getConditionAndOutcomesBySource();

        assertThat(outcomes.get(TASK_EXECUTION).isFullMatch()).isTrue();
        assertThat(outcomes.get("org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration").isFullMatch()).isFalse();
        assertThat(outcomes.get("org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration").isFullMatch())
                .isFalse();
        // klucze z '#' to pojedyncze metody @Bean:
        assertThat(outcomes).containsKey(
                "org.springframework.boot.autoconfigure.task.TaskExecutorConfigurations$TaskExecutorConfiguration#applicationTaskExecutor");
    }

    @Test
    @DisplayName("6. spring.autoconfigure.exclude wyłącza autokonfigurację; literówka jest cicho ignorowana (pytanie 4)")
    void excludeByProperty() {
        try (var excluded = start("spring.autoconfigure.exclude=" + TASK_EXECUTION)) {
            assertThat(excluded.containsBean("applicationTaskExecutor")).isFalse();
            assertThat(ConditionEvaluationReport.get(excluded.getBeanFactory()).getExclusions()).contains(TASK_EXECUTION);
        }
        String typo = "org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfigration";
        try (var withTypo = start("spring.autoconfigure.exclude=" + typo)) {
            assertThat(withTypo.containsBean("applicationTaskExecutor")).as("literówka niczego nie wyłączyła").isTrue();
        }
    }

    @Test
    @DisplayName("6. Wykluczenie klasy, która nie jest autokonfiguracją, przerywa start (pytanie 5)")
    void excludeNonAutoConfigurationFails() {
        assertThatThrownBy(() -> start("spring.autoconfigure.exclude=java.lang.String").close())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be excluded because they are not auto-configuration classes");
    }

    private static ConfigurableApplicationContext start(String property) {
        return new SpringApplicationBuilder(Boot01Application.class)
                .web(WebApplicationType.NONE)
                .properties(property)
                .run();
    }
}
