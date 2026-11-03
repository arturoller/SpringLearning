package s03_boot_basics.boot01_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Boot01AutoConfiguration. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Boot01AutoConfigurationExercisesTest {

    private static final String JDBC = "org.springframework.boot.jdbc.autoconfigure.";

    private static ConfigurableApplicationContext start(String... properties) {
        return new SpringApplicationBuilder(Boot01Application.class)
                .web(WebApplicationType.NONE)
                .properties(properties)
                .run();
    }

    @Test
    @DisplayName("Ćwiczenie 1: ustawienie spring.autoconfigure.exclude wyłącza DataSource i JdbcTemplate")
    void exercise1() {
        try (ConfigurableApplicationContext app = start(Boot01AutoConfiguration.exercise1ExcludeProperty())) {
            assertThat(app.getBeanNamesForType(DataSource.class)).isEmpty();
            assertThat(app.getBeanNamesForType(JdbcTemplate.class)).isEmpty();
        }
    }

    @Test
    @DisplayName("Ćwiczenie 2: przed własnym DataSource autokonfiguracja ustępuje")
    void exercise2() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class))
                .withBean(DataSource.class, Boot01AutoConfiguration::exercise2OwnDataSource)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(DataSource.class);
                    DataSource dataSource = ctx.getBean(DataSource.class);
                    try (Connection connection = dataSource.getConnection()) {
                        assertThat(connection.getMetaData().getURL()).isEqualTo("jdbc:h2:mem:boot01-cw2");
                    }
                    assertThat(ctx.getBean(JdbcTemplate.class).getDataSource()).isSameAs(dataSource);
                });
    }

    @Test
    @DisplayName("Ćwiczenie 3: odczyt z raportu, czy autokonfiguracja zadziałała")
    void exercise3() {
        try (ConfigurableApplicationContext app = start()) {
            ConditionEvaluationReport report = ConditionEvaluationReport.get(app.getBeanFactory());
            assertThat(Boot01AutoConfiguration.exercise3Matched(report, JDBC + "DataSourceAutoConfiguration")).isTrue();
            assertThat(Boot01AutoConfiguration.exercise3Matched(report, JDBC + "JndiDataSourceAutoConfiguration")).isFalse();
            assertThat(Boot01AutoConfiguration.exercise3Matched(report, "nie.ma.TakiejKlasy")).isFalse();
        }
    }

    @Test
    @DisplayName("Ćwiczenie 4: posortowana lista autokonfiguracji JDBC, które nie zadziałały")
    void exercise4() {
        try (ConfigurableApplicationContext app = start()) {
            ConditionEvaluationReport report = ConditionEvaluationReport.get(app.getBeanFactory());
            assertThat(Boot01AutoConfiguration.exercise4NegativeJdbcMatches(report))
                    .containsExactly("JndiDataSourceAutoConfiguration", "XADataSourceAutoConfiguration");
        }
    }
}
