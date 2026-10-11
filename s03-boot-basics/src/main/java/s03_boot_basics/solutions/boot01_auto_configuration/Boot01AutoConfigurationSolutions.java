package s03_boot_basics.solutions.boot01_auto_configuration;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import s03_boot_basics.boot01_auto_configuration.Boot01Application;
import s03_boot_basics.boot01_auto_configuration.Boot01OwnExecutorConfig;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

/** Rozwiązania wzorcowe ćwiczeń lekcji Boot01AutoConfiguration — zajrzyj dopiero po własnej próbie! */
public final class Boot01AutoConfigurationSolutions {

    private Boot01AutoConfigurationSolutions() {
    }

    static List<String> solution1(ApplicationContext context) {
        return Arrays.stream(context.getBeanNamesForType(Executor.class)).sorted().toList();
    }

    static List<String> solution2(ConditionEvaluationReport report) {
        return report.getConditionAndOutcomesBySource().entrySet().stream()
                .filter(entry -> !entry.getKey().contains("$") && !entry.getKey().contains("#"))
                .filter(entry -> entry.getValue().isFullMatch())
                .map(entry -> entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1))
                .sorted()
                .toList();
    }

    static ConfigurableApplicationContext solution3() {
        return new SpringApplicationBuilder(Boot01Application.class, Boot01OwnExecutorConfig.class)
                .web(WebApplicationType.NONE)
                .properties("spring.jmx.enabled=true")
                .run();
    }
}
