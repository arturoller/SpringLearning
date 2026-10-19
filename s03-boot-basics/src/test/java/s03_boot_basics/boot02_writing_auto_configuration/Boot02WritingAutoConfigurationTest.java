package s03_boot_basics.boot02_writing_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.DefaultResourceLoader;
import s03_boot_basics.boot01_auto_configuration.Boot01Application;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Boot02WritingAutoConfiguration. Autokonfigurację testujemy tak jak autorzy Boota:
 * {@link ApplicationContextRunner} + {@code AutoConfigurations.of(...)} — mały kontekst z jedną autokonfiguracją,
 * w którym w każdym wierszu zmieniamy jedną rzecz (ustawienie, bean użytkownika, ładowacz klas).
 * Na końcu dwie pełne aplikacje: Boot02Application (z profilem boot02) i aplikacja INNEJ lekcji (Boot01Application).
 */
class Boot02WritingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ShopGreeterAutoConfiguration.class));

    /** Konfiguracja „aplikacji użytkownika” z własnym ShopGreeter (bez @Configuration — nie trafi do skanowania). */
    static class UserGreeterConfig {
        @Bean
        ShopGreeter myGreeter() {
            return new ShopGreeter("Sklep Użytkownika", "Cześć");
        }
    }

    /** Raport powitań — przykład beana, który ma sens tylko wtedy, gdy istnieje ShopGreeter. */
    record GreetingReport(ShopGreeter greeter) {
    }

    /** Autokonfiguracja z @ConditionalOnBean, ale BEZ ustalonej kolejności. */
    @AutoConfiguration
    static class ReportWithoutOrderAutoConfiguration {
        @Bean
        @ConditionalOnBean(ShopGreeter.class)
        GreetingReport greetingReport(ShopGreeter greeter) {
            return new GreetingReport(greeter);
        }
    }

    /** To samo, ale „po” ShopGreeterAutoConfiguration. */
    @AutoConfiguration(after = ShopGreeterAutoConfiguration.class)
    static class ReportAfterGreeterAutoConfiguration {
        @Bean
        @ConditionalOnBean(ShopGreeter.class)
        GreetingReport greetingReport(ShopGreeter greeter) {
            return new GreetingReport(greeter);
        }
    }

    private static ConfigurableApplicationContext start(Class<?> source, String... profiles) {
        return new SpringApplicationBuilder(source)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .profiles(profiles)
                .run();
    }

    private static List<String> messages(ConditionAndOutcomes outcomes) {
        return outcomes.stream().map(o -> o.getOutcome().getMessage()).toList();
    }

    @Test
    @DisplayName("1. Domyślnie wyłączona: bez s03.greeter.enabled=true nie ma ani ShopGreeter, ani jego ustawień")
    void disabledByDefault() {
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ShopGreeter.class);
            assertThat(ctx).doesNotHaveBean(ShopGreeterProperties.class);
        });
        // @ConditionalOnBooleanProperty: „tak” to nie „true”
        runner.withPropertyValues("s03.greeter.enabled=tak").run(ctx -> assertThat(ctx).doesNotHaveBean(ShopGreeter.class));
    }

    @Test
    @DisplayName("2. Włączona właściwością: ShopGreeter powstaje, a s03.greeter.* trafia do niego przez rekord ustawień")
    void enabledByProperty() {
        runner.withPropertyValues("s03.greeter.enabled=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(ShopGreeter.class);
            assertThat(ctx.getBean(ShopGreeter.class).greet("Jan Kowalski"))
                    .isEqualTo("Dzień dobry, Jan Kowalski! Witamy w sklepie SpringLearning.");   // @DefaultValue
        });
        runner.withPropertyValues("s03.greeter.enabled=true", "s03.greeter.shop-name=SpringLearning Gdańsk",
                        "s03.greeter.greeting=Witaj")
                .run(ctx -> assertThat(ctx.getBean(ShopGreeter.class).greet("Adam Mazur"))
                        .isEqualTo("Witaj, Adam Mazur! Witamy w sklepie SpringLearning Gdańsk."));
    }

    @Test
    @DisplayName("3. Bean użytkownika wygrywa; brak klasy biblioteki (FilteredClassLoader) wyłącza autokonfigurację")
    void userBeanWinsAndMissingClassDisables() {
        runner.withPropertyValues("s03.greeter.enabled=true", "s03.greeter.greeting=Witaj")
                .withUserConfiguration(UserGreeterConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ShopGreeter.class);
                    assertThat(ctx).hasBean("myGreeter").doesNotHaveBean("shopGreeter");
                    assertThat(ctx.getBean(ShopGreeter.class).greet("Ewa Lis"))
                            .isEqualTo("Cześć, Ewa Lis! Witamy w sklepie Sklep Użytkownika.");
                });

        runner.withPropertyValues("s03.greeter.enabled=true")
                .withClassLoader(new FilteredClassLoader(ShopGreeter.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ShopGreeter.class));
    }

    @Test
    @DisplayName("4. Kolejność: @ConditionalOnBean bez after nie widzi beana innej autokonfiguracji; z after — widzi")
    void orderingWithAfter() {
        // bez after: kolejność alfabetyczna — „Boot02…Test$Report…” jest przed „ShopGreeter…”, więc warunek
        // @ConditionalOnBean(ShopGreeter.class) jest liczony, zanim ShopGreeter w ogóle zostanie zarejestrowany
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ShopGreeterAutoConfiguration.class,
                        ReportWithoutOrderAutoConfiguration.class))
                .withPropertyValues("s03.greeter.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ShopGreeter.class);
                    assertThat(ctx).doesNotHaveBean(GreetingReport.class);
                });

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ShopGreeterAutoConfiguration.class,
                        ReportAfterGreeterAutoConfiguration.class))
                .withPropertyValues("s03.greeter.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(GreetingReport.class));
    }

    @Test
    @DisplayName("5. Dlaczego nie skanowanie: autokonfiguracja wczytana jak zwykła konfiguracja PRZED Twoją → dwa beany")
    void whyNotComponentScan() {
        // ŹLE: autokonfiguracja jako zwykła konfiguracja, przetworzona przed konfiguracją użytkownika
        new ApplicationContextRunner()
                .withUserConfiguration(ShopGreeterAutoConfiguration.class, UserGreeterConfig.class)
                .withPropertyValues("s03.greeter.enabled=true")
                .run(ctx -> assertThat(ctx.getBeanNamesForType(ShopGreeter.class))
                        .containsExactlyInAnyOrder("shopGreeter", "myGreeter"));

        // DOBRZE: ta sama para, ale autokonfiguracja „swoimi drzwiami” — zawsze po konfiguracji użytkownika
        runner.withUserConfiguration(UserGreeterConfig.class)
                .withPropertyValues("s03.greeter.enabled=true")
                .run(ctx -> assertThat(ctx.getBeanNamesForType(ShopGreeter.class)).containsExactly("myGreeter"));
    }

    @Test
    @DisplayName("6. Pełna aplikacja z profilem boot02: plik .imports działa, klasa nie pochodzi ze skanowania; literówka w .imports = błąd startu")
    void importsFileRegistersTheAutoConfiguration(@TempDir Path extraClasspath) throws IOException {
        try (ConfigurableApplicationContext ctx = start(Boot02Application.class, "boot02")) {
            assertThat(ctx.getBean(ShopGreeter.class).greet("Zofia Krawczyk"))
                    .isEqualTo("Witaj, Zofia Krawczyk! Witamy w sklepie SpringLearning Warszawa.");

            // import z pliku .imports → nazwa beana = pełna nazwa klasy; skanowanie dałoby „shopGreeterAutoConfiguration”
            assertThat(ctx.containsBean(ShopGreeterAutoConfiguration.class.getName())).isTrue();
            assertThat(ctx.containsBean("shopGreeterAutoConfiguration")).isFalse();

            // klasy z @AutoConfiguration w skanowanym pakiecie (tu: zagnieżdżone w tym teście) też są pomijane
            assertThat(ctx.getBeanNamesForType(ReportWithoutOrderAutoConfiguration.class)).isEmpty();
            assertThat(ctx.getBeanNamesForType(ReportAfterGreeterAutoConfiguration.class)).isEmpty();

            ConditionAndOutcomes outcomes = ConditionEvaluationReport.get(ctx.getBeanFactory())
                    .getConditionAndOutcomesBySource().get(ShopGreeterAutoConfiguration.class.getName());
            assertThat(outcomes.isFullMatch()).isTrue();
        }

        assertTypoInImportsFileFailsStartup(extraClasspath);
    }

    /** Aplikacja „tylko autokonfiguracja” — bez skanowania (brak @Configuration, więc też nikt jej nie zeskanuje). */
    @EnableAutoConfiguration
    static class AutoConfigurationOnly {
    }

    /** PUŁAPKA z sekcji 6: literówka w pliku .imports — start się nie udaje. */
    private void assertTypoInImportsFileFailsStartup(Path extraClasspath) throws IOException {
        // prawdziwy plik .imports z literówką, dołożony do ścieżki klas (katalog tymczasowy + własny ładowacz klas)
        Path imports = extraClasspath.resolve(
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
        Files.createDirectories(imports.getParent());
        Files.writeString(imports, "s03_boot_basics.boot02_writing_auto_configuration.ShopGreeterAutoConfigurtion\n");

        try (URLClassLoader classLoader = new URLClassLoader(new URL[] {extraClasspath.toUri().toURL()},
                getClass().getClassLoader())) {
            SpringApplication application = new SpringApplication(new DefaultResourceLoader(classLoader),
                    AutoConfigurationOnly.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setBannerMode(Banner.Mode.OFF);
            application.setLogStartupInfo(false);

            assertThatThrownBy(application::run)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unable to read meta-data for class "
                            + "s03_boot_basics.boot02_writing_auto_configuration.ShopGreeterAutoConfigurtion")
                    .hasRootCauseInstanceOf(FileNotFoundException.class);
        }
    }

    @Test
    @DisplayName("7. Aplikacja INNEJ lekcji (Boot01Application) też czyta plik .imports, ale bez właściwości nic nie dostaje")
    void otherLessonsAreNotAffected() {
        try (ConfigurableApplicationContext ctx = start(Boot01Application.class)) {
            assertThat(ctx.getBeanNamesForType(ShopGreeter.class)).isEmpty();

            ConditionAndOutcomes outcomes = ConditionEvaluationReport.get(ctx.getBeanFactory())
                    .getConditionAndOutcomesBySource().get(ShopGreeterAutoConfiguration.class.getName());
            assertThat(outcomes.isFullMatch()).isFalse();
            assertThat(messages(outcomes)).contains(
                    "@ConditionalOnBooleanProperty (s03.greeter.enabled=true) did not find property 's03.greeter.enabled'");
        }
    }
}
