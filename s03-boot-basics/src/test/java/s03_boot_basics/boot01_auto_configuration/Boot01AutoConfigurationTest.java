package s03_boot_basics.boot01_auto_configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Boot01AutoConfiguration — jeden test na sekcję.
 * <p>
 * Sekcja 1 używa {@code @SpringBootTest}, bo chcemy zobaczyć PRAWDZIWĄ aplikację: co Boot zrobił sam z siebie.
 * Pozostałe sekcje używają {@link ApplicationContextRunner} (uruchamiacz kontekstu) — lżejszego narzędzia, które
 * buduje mały kontekst tylko z wybranych autokonfiguracji. Dzięki temu w jednym teście sprawdzisz kilka wariantów
 * (z bazą, bez bazy, z Twoim beanem) w ułamku sekundy i bez zmieniania classpath.
 * <p>
 * PUŁAPKA: {@code classes = Boot01Application.class} jest tu KONIECZNE. Test ma klasy wewnętrzne z {@code @Configuration}
 * (dla sekcji 3 i 5), a {@code @SpringBootTest} bez {@code classes} użyłby właśnie ich ZAMIAST klasy startowej
 * aplikacji — w kontekście nie byłoby autokonfiguracji z Boot01Application. Skan komponentów aplikacji tych klas
 * wewnętrznych nie widzi (Boot pomija klasy zagnieżdżone w klasach testowych).
 */
@SpringBootTest(classes = Boot01Application.class)
class Boot01AutoConfigurationTest {

    private static final String JDBC = "org.springframework.boot.jdbc.autoconfigure.";

    @Autowired
    ConfigurableApplicationContext context;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JdbcClient jdbcClient;

    /** Uruchamiacz z autokonfiguracjami JDBC — wspólny początek sekcji 2, 3 i 5. */
    private final ApplicationContextRunner jdbcRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class));

    /** Konfiguracja UŻYTKOWNIKA z własnym DataSource (sekcja 3). Klasa wewnętrzna testu — skan aplikacji jej nie widzi. */
    @Configuration(proxyBeanMethods = false)
    static class ShopDataSourceConfiguration {
        @Bean
        DataSource shopDataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:boot01-shop", "sa", "");
        }
    }

    /** Odpowiednik {@code @SpringBootApplication(exclude = ...)} bez skanowania pakietów (sekcja 5). */
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    static class AppWithoutDataSource {
    }

    private static String urlOf(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL();
        }
    }

    @Test
    @DisplayName("1. Bez jednej linijki konfiguracji w kontekście są DataSource, JdbcTemplate i JdbcClient, a zapytanie działa")
    void section1_beansNobodyWrote() throws SQLException {
        // @SpringBootApplication to trzy adnotacje w jednej
        assertThat(AnnotatedElementUtils.hasAnnotation(Boot01Application.class, SpringBootConfiguration.class)).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(Boot01Application.class, EnableAutoConfiguration.class)).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(Boot01Application.class,
                org.springframework.context.annotation.ComponentScan.class)).isTrue();

        // DataSource to pula połączeń HikariCP do bazy H2 w pamięci o wygenerowanej, unikalnej nazwie
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        assertThat(urlOf(dataSource)).startsWith("jdbc:h2:mem:");

        // najprostsze zapytania — przez JdbcTemplate i przez JdbcClient
        assertThat(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
        assertThat(jdbcClient.sql("SELECT H2VERSION()").query(String.class).single()).startsWith("2.");
    }

    @Test
    @DisplayName("2. Warunki: DataSource powstaje tylko, gdy na classpath jest baza albo pula połączeń")
    void section2_conditions() {
        // a) wszystko na miejscu → jest DataSource (Hikari) i JdbcTemplate
        jdbcRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(DataSource.class).hasSingleBean(JdbcTemplate.class);
            assertThat(ctx.getBean(DataSource.class)).isInstanceOf(HikariDataSource.class);
        });

        // b) ukrywamy H2 i Hikari → nie ma czego skonfigurować → brak DataSource, a więc brak JdbcTemplate
        jdbcRunner.withClassLoader(new FilteredClassLoader("org.h2", "com.zaxxer.hikari"))
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).doesNotHaveBean(DataSource.class).doesNotHaveBean(JdbcTemplate.class);
                });

        // c) ukrywamy TYLKO H2 → Hikari jest, więc Boot próbuje zbudować pulę, ale nie zna adresu bazy → start się wywraca
        jdbcRunner.withClassLoader(new FilteredClassLoader("org.h2"))
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("Failed to determine a suitable driver class"));

        // d) JdbcTemplateAutoConfiguration bez DataSourceAutoConfiguration → warunek „jest DataSource” nie spełniony
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(JdbcTemplate.class));

        // e) @ConditionalOnProperty: spring.datasource.type wybiera rodzaj puli; inny typ niż Hikari → brak puli Hikari
        jdbcRunner.withPropertyValues("spring.datasource.type=" + DriverManagerDataSource.class.getName(),
                        "spring.datasource.url=jdbc:h2:mem:boot01-type")
                .run(ctx -> assertThat(ctx.getBean(DataSource.class)).isInstanceOf(DriverManagerDataSource.class));
    }

    @Test
    @DisplayName("3. Back-off: Twój DataSource wygrywa, autokonfiguracja ustępuje, a JdbcTemplate używa Twojego beana")
    void section3_backOff() {
        jdbcRunner.withUserConfiguration(ShopDataSourceConfiguration.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(DataSource.class);
            assertThat(ctx).hasBean("shopDataSource").doesNotHaveBean("dataSource");
            DataSource only = ctx.getBean(DataSource.class);
            assertThat(only).isInstanceOf(DriverManagerDataSource.class);
            assertThat(urlOf(only)).isEqualTo("jdbc:h2:mem:boot01-shop");
            assertThat(ctx.getBean(JdbcTemplate.class).getDataSource()).isSameAs(only);
        });
    }

    @Test
    @DisplayName("4. Raport warunków: DataSourceAutoConfiguration zadziałała, JndiDataSourceAutoConfiguration nie")
    void section4_conditionReport() {
        ConditionEvaluationReport report = ConditionEvaluationReport.get(context.getBeanFactory());

        assertThat(report.getConditionAndOutcomesBySource().get(JDBC + "DataSourceAutoConfiguration").isFullMatch())
                .as("DataSourceAutoConfiguration powinna być w Positive matches").isTrue();

        ConditionEvaluationReport.ConditionAndOutcomes jndi =
                report.getConditionAndOutcomesBySource().get(JDBC + "JndiDataSourceAutoConfiguration");
        assertThat(jndi.isFullMatch()).as("JNDI powinno być w Negative matches").isFalse();
        assertThat(jndi.stream().map(o -> o.getOutcome().getMessage()))
                .anySatisfy(message -> assertThat(message).contains("spring.datasource.jndi-name"));
    }

    @Test
    @DisplayName("5. Wyłączanie: exclude w adnotacji i spring.autoconfigure.exclude usuwają DataSource z kontekstu")
    void section5_exclude() {
        // a) atrybut exclude (ten sam co w @SpringBootApplication)
        new ApplicationContextRunner().withUserConfiguration(AppWithoutDataSource.class).run(ctx -> {
            assertThat(ctx).hasNotFailed().doesNotHaveBean(DataSource.class).doesNotHaveBean(JdbcTemplate.class);
            assertThat(ConditionEvaluationReport.get(ctx.getBeanFactory()).getExclusions())
                    .contains(DataSourceAutoConfiguration.class.getName());
        });

        // b) właściwość spring.autoconfigure.exclude — prawdziwa aplikacja lekcji, bez zmiany kodu
        try (ConfigurableApplicationContext app = new SpringApplicationBuilder(Boot01Application.class)
                .web(WebApplicationType.NONE)
                .properties("spring.autoconfigure.exclude=" + DataSourceAutoConfiguration.class.getName())
                .run()) {
            assertThat(app.getBeanNamesForType(DataSource.class)).isEmpty();
            assertThat(app.getBeanNamesForType(JdbcTemplate.class)).isEmpty();
        }

        // c) PUŁAPKA: ApplicationContextRunner z AutoConfigurations.of(...) NIE czyta spring.autoconfigure.exclude
        jdbcRunner.withPropertyValues("spring.autoconfigure.exclude=" + DataSourceAutoConfiguration.class.getName())
                .run(ctx -> assertThat(ctx).hasSingleBean(DataSource.class));
    }
}
