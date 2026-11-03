package s03_boot_basics.boot02_writing_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import s03_boot_basics.boot01_auto_configuration.Boot01Application;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Boot02WritingAutoConfiguration — jeden test na sekcję.
 * <p>
 * Większość sprawdzeń robi {@link ApplicationContextRunner} (uruchamiacz kontekstu — opis w lekcji
 * s03_boot_basics/Boot01AutoConfiguration): to STANDARDOWY sposób testowania autokonfiguracji, tak testuje je sam Boot.
 * Sekcja 5 dokłada jeden {@code @SpringBootTest}, bo tylko prawdziwa aplikacja dowodzi, że plik imports działa.
 * <p>
 * {@code classes = Boot02Application.class} — konieczne, bo test ma wewnętrzne klasy {@code @Configuration}
 * (bez tego {@code @SpringBootTest} użyłby ich zamiast klasy startowej; patrz test lekcji Boot01).
 */
@SpringBootTest(classes = Boot02Application.class)
@ActiveProfiles("boot02")
class Boot02WritingAutoConfigurationTest {

    private static final BigDecimal MUG = new BigDecimal("12.5");

    @Autowired
    ConfigurableApplicationContext context;

    @Autowired
    ProductLabels labels;

    /** Biblioteka w izolacji: tylko nasza autokonfiguracja cen. */
    private final ApplicationContextRunner priceRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PriceAutoConfiguration.class));

    /** ZŁA autokonfiguracja (sekcja 4): ocenia @ConditionalOnBean PRZED tym, jak powstaje PriceFormatter. */
    @AutoConfiguration(before = PriceAutoConfiguration.class)
    @ConditionalOnBean(PriceFormatter.class)
    static class TooEarlyTagAutoConfiguration {
        @Bean
        PriceTagPrinter tooEarlyPriceTagPrinter(PriceFormatter formatter) {
            return new PriceTagPrinter(formatter);
        }
    }

    /** ZŁA konfiguracja użytkownika (sekcja 4): @ConditionalOnBean na beanie, który da dopiero autokonfiguracja. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(PriceFormatter.class)
    static class UserConfigurationWaitingForFormatter {
        @Bean
        PriceTagPrinter userPriceTagPrinter(PriceFormatter formatter) {
            return new PriceTagPrinter(formatter);
        }
    }

    @Test
    @DisplayName("1. @AutoConfiguration + @EnableConfigurationProperties: formater z ustawień domyślnych i własnych")
    void section1_autoConfigurationWithProperties() {
        priceRunner.withPropertyValues("boot02.price.enabled=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(PriceFormatter.class).hasSingleBean(PriceProperties.class);
            assertThat(ctx.getBean(PriceFormatter.class).format(MUG)).isEqualTo("12,50 zł");
        });

        priceRunner.withPropertyValues("boot02.price.enabled=true", "boot02.price.currency=EUR", "boot02.price.decimals=0")
                .run(ctx -> assertThat(ctx.getBean(PriceFormatter.class).format(MUG)).isEqualTo("13 EUR"));
    }

    @Test
    @DisplayName("2. Plik imports rejestruje klasy, a przełącznik enabled trzyma bibliotekę wyłączoną w innych lekcjach")
    void section2_registrationAndIsolation() {
        // plik META-INF/spring/…AutoConfiguration.imports jest na classpath i wymienia obie klasy
        ImportCandidates candidates = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader());
        assertThat(candidates.getCandidates())
                .contains(PriceAutoConfiguration.class.getName(), PriceTagAutoConfiguration.class.getName());

        // bez klucza i z wartością inną niż "true" — biblioteka nic nie robi
        priceRunner.run(ctx -> assertThat(ctx).doesNotHaveBean(PriceFormatter.class).doesNotHaveBean(PriceProperties.class));
        priceRunner.withPropertyValues("boot02.price.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(PriceFormatter.class));
        priceRunner.withPropertyValues("boot02.price.enabled=yes")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(PriceFormatter.class));
        // wielkość liter w havingValue nie ma znaczenia: "TRUE" też włącza
        priceRunner.withPropertyValues("boot02.price.enabled=TRUE")
                .run(ctx -> assertThat(ctx).hasSingleBean(PriceFormatter.class));

        // aplikacja INNEJ lekcji (Boot01) też czyta ten plik imports — ale bez przełącznika nic nie dostaje
        try (ConfigurableApplicationContext other = new SpringApplicationBuilder(Boot01Application.class)
                .web(WebApplicationType.NONE).run()) {
            assertThat(other.getBeanNamesForType(PriceFormatter.class)).isEmpty();
            assertThat(other.getBeanNamesForType(PriceTagPrinter.class)).isEmpty();
            ConditionEvaluationReport report = ConditionEvaluationReport.get(other.getBeanFactory());
            assertThat(report.getConditionAndOutcomesBySource().get(PriceAutoConfiguration.class.getName()).isFullMatch())
                    .as("w Boot01 PriceAutoConfiguration jest oceniana, ale trafia do Negative matches").isFalse();
        }
    }

    @Test
    @DisplayName("3. Back-off: PriceFormatter aplikacji wygrywa, a etykiety z biblioteki używają właśnie jego")
    void section3_backOff() {
        PriceFormatter mine = new PriceFormatter("złotych", 1);
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PriceAutoConfiguration.class, PriceTagAutoConfiguration.class))
                .withPropertyValues("boot02.price.enabled=true")
                .withBean("shopPriceFormatter", PriceFormatter.class, () -> mine)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(PriceFormatter.class).hasBean("shopPriceFormatter").doesNotHaveBean("priceFormatter");
                    assertThat(ctx.getBean(PriceFormatter.class)).isSameAs(mine);
                    assertThat(ctx.getBean(PriceTagPrinter.class).tag("Kubek", MUG)).isEqualTo("[Kubek | 12,5 złotych]");
                });
    }

    @Test
    @DisplayName("4. Kolejność: after = … gwarantuje @ConditionalOnBean; before i zwykła @Configuration go nie widzą")
    void section4_ordering() {
        // dobrze: kolejność argumentów odwrotna, ale after = PriceAutoConfiguration ustawia ją poprawnie
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PriceTagAutoConfiguration.class, PriceAutoConfiguration.class))
                .withPropertyValues("boot02.price.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(PriceTagPrinter.class));

        // źle: before = PriceAutoConfiguration → w chwili oceny warunku formatera jeszcze nie ma
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PriceAutoConfiguration.class, TooEarlyTagAutoConfiguration.class))
                .withPropertyValues("boot02.price.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(PriceFormatter.class).doesNotHaveBean(PriceTagPrinter.class));

        // źle: @ConditionalOnBean w konfiguracji UŻYTKOWNIKA — oceniany przed wszystkimi autokonfiguracjami
        priceRunner.withPropertyValues("boot02.price.enabled=true")
                .withUserConfiguration(UserConfigurationWaitingForFormatter.class)
                .run(ctx -> assertThat(ctx).hasSingleBean(PriceFormatter.class).doesNotHaveBean(PriceTagPrinter.class));
    }

    @Test
    @DisplayName("5. Prawdziwa aplikacja z boot02.price.enabled=true dostaje beany biblioteki z pliku imports")
    void section5_realApplication() {
        // ustawienia z application-boot02.properties: enabled=true, currency=PLN
        assertThat(labels.priceOf(new BigDecimal("19.9"))).isEqualTo("19,90 PLN");
        assertThat(labels.tagOf("Kubek", MUG)).isEqualTo("[Kubek | 12,50 PLN]");

        ConditionEvaluationReport report = ConditionEvaluationReport.get(context.getBeanFactory());
        assertThat(report.getConditionAndOutcomesBySource().get(PriceAutoConfiguration.class.getName()).isFullMatch()).isTrue();
        assertThat(report.getConditionAndOutcomesBySource().get(PriceTagAutoConfiguration.class.getName()).isFullMatch()).isTrue();
        // AutoConfigurationExcludeFilter: skan pakietu lekcji NIE zarejestrował klasy drugi raz jako zwykłej konfiguracji
        assertThat(context.getBeanNamesForType(PriceAutoConfiguration.class)).hasSize(1);
    }
}
