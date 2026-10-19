package s03_boot_basics.boot03_spring_application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Boot03SpringApplication. Tu NIE używamy {@code @SpringBootTest}: lekcja jest o samym starcie,
 * więc każdy test uruchamia {@link SpringApplication} sam — z wybranymi ustawieniami i argumentami — i zamyka
 * kontekst (try-with-resources), żeby kolejne testy startowały od zera.
 */
class Boot03SpringApplicationTest {

    /** Aplikacja lekcji „po cichu”: bez banera i bez logu startu — tam, gdzie test ich nie ogląda. */
    private static SpringApplication quietApplication() {
        SpringApplication application = new SpringApplication(Boot03Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        return application;
    }

    /** Konfiguracja z runnerem, który rzuca wyjątek KONTROLOWANY (bez @Configuration — nie trafi do skanowania). */
    static class CheckedFailingRunnerConfig {
        @Bean
        ApplicationRunner failingRunner() {
            return args -> {
                throw new IOException("Brak pliku z cennikiem");
            };
        }
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("1. run(): typ aplikacji NONE wybrany ze ścieżki klas; „Started …” w logu PRZED pracą runnerów")
    void runStepByStep(CapturedOutput output) {
        SpringApplication application = new SpringApplication(Boot03Application.class);
        assertThat(application.getWebApplicationType()).isEqualTo(WebApplicationType.NONE);   // brak serwletów i WebFluxa
        application.setBannerMode(Banner.Mode.OFF);
        // Nazwę w „Starting …/Started …” Boot bierze z klasy, której metoda main uruchomiła JVM (szuka jej na stosie
        // wywołań). W teście to klasa narzędzia testowego (np. ForkedBooter z Mavena), więc ustawiamy ją jawnie.
        application.setMainApplicationClass(Boot03Application.class);

        try (ConfigurableApplicationContext context = application.run("--limit=3")) {
            assertThat(context).isInstanceOf(AnnotationConfigApplicationContext.class);   // zwykły kontekst, nie webowy
            assertThat(context.getBean(ProductCatalog.class).loaded()).hasSize(3);
        }
        String log = output.getOut();
        assertThat(log).contains("Starting Boot03Application using Java", "Started Boot03Application in",
                "Wczytano produktów: 3");
        assertThat(log.indexOf("Started Boot03Application in")).isLessThan(log.indexOf("Wczytano produktów: 3"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("2. Dostosowanie: baner, domyślne ustawienia, dodatkowe profile, leniwe beany, SpringApplicationBuilder")
    void customizeTheStartup(CapturedOutput output) {
        // baner włączony
        SpringApplication withBanner = quietApplication();
        withBanner.setBannerMode(Banner.Mode.CONSOLE);
        try (ConfigurableApplicationContext context = withBanner.run()) {
            assertThat(output.getOut()).contains(":: Spring Boot ::", "(v" + SpringBootVersion.getVersion() + ")");
        }

        // domyślne ustawienia (najniższy priorytet) i dodatkowe profile
        SpringApplication defaults = quietApplication();
        defaults.setDefaultProperties(Map.of("s03.boot03.shop-name", "z ustawień domyślnych"));
        defaults.setAdditionalProfiles("boot03-extra");
        try (ConfigurableApplicationContext context = defaults.run()) {
            assertThat(context.getEnvironment().getProperty("s03.boot03.shop-name")).isEqualTo("z ustawień domyślnych");
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("boot03-extra");
        }
        try (ConfigurableApplicationContext context = defaults.run("--s03.boot03.shop-name=z argumentu")) {
            assertThat(context.getEnvironment().getProperty("s03.boot03.shop-name")).isEqualTo("z argumentu");
        }

        // leniwe tworzenie beanów: MonthlyReportGenerator powstaje dopiero przy pierwszym getBean
        SpringApplication lazy = quietApplication();
        lazy.setLazyInitialization(true);
        try (ConfigurableApplicationContext context = lazy.run()) {
            StartupJournal journal = context.getBean(StartupJournal.class);
            assertThat(journal.entries()).doesNotContain("utworzono MonthlyReportGenerator");
            context.getBean(MonthlyReportGenerator.class);
            assertThat(journal.entries()).contains("utworzono MonthlyReportGenerator");
        }
        try (ConfigurableApplicationContext context = quietApplication().run()) {   // bez lazy — bean jest od startu
            assertThat(context.getBean(StartupJournal.class).entries()).contains("utworzono MonthlyReportGenerator");
        }

        // to samo płynnym API (fluent API): SpringApplicationBuilder; properties(...) = ustawienia domyślne
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Boot03Application.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .profiles("boot03-extra")
                .properties("s03.boot03.shop-name=z buildera")
                .lazyInitialization(true)
                .run()) {
            assertThat(context.getEnvironment().getProperty("s03.boot03.shop-name")).isEqualTo("z buildera");
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("boot03-extra");
        }
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("3. PUŁAPKA: ustawienie spring.main.banner-mode wygrywa z setBannerMode(...) w kodzie")
    void springMainPropertiesWinOverSetters(CapturedOutput output) {
        SpringApplication application = quietApplication();
        application.setBannerMode(Banner.Mode.CONSOLE);   // w kodzie: pokaż baner…
        try (ConfigurableApplicationContext context = application.run("--spring.main.banner-mode=off")) {   // …a tu: nie
            assertThat(context.isActive()).isTrue();
        }
        assertThat(output.getOut()).doesNotContain(":: Spring Boot ::");

        SpringApplication lazyByProperty = quietApplication();   // w kodzie nie ma setLazyInitialization
        try (ConfigurableApplicationContext context = lazyByProperty.run("--spring.main.lazy-initialization=true")) {
            assertThat(context.getBean(StartupJournal.class).entries()).doesNotContain("utworzono MonthlyReportGenerator");
        }
    }

    @Test
    @DisplayName("4. Runnery: ApplicationRunner dostaje opcje rozłożone, CommandLineRunner — surowe; kolejność z @Order")
    void runners() {
        try (ConfigurableApplicationContext context = quietApplication().run("--limit=2", "plik.csv")) {
            List<String> runnerEntries = context.getBean(StartupJournal.class).entries().stream()
                    .filter(entry -> entry.startsWith("runner"))
                    .toList();
            assertThat(runnerEntries).containsExactly(
                    "runner 1 (ApplicationRunner): opcje [limit], bez opcji [plik.csv], wczytano 2",
                    "runner 2 (CommandLineRunner): argumenty [--limit=2, plik.csv], w katalogu 2");
        }
    }

    @Test
    @DisplayName("5. Wyjątek w runnerze zatrzymuje start: niekontrolowany leci bez zmian, kontrolowany — opakowany")
    void exceptionInRunnerStopsTheStartup() {
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        SpringApplication application = quietApplication();
        application.addListeners(event -> events.add(event.getClass().getSimpleName()));

        assertThatThrownBy(() -> application.run("--limit=abc"))
                .isInstanceOf(NumberFormatException.class)
                .hasMessage("For input string: \"abc\"");
        assertThat(events).contains("ApplicationStartedEvent", "ApplicationFailedEvent", "ContextClosedEvent")
                .doesNotContain("ApplicationReadyEvent");

        SpringApplication checked = new SpringApplication(CheckedFailingRunnerConfig.class);
        checked.setWebApplicationType(WebApplicationType.NONE);
        checked.setBannerMode(Banner.Mode.OFF);
        checked.setLogStartupInfo(false);
        assertThatThrownBy(checked::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to execute ApplicationRunner")
                .cause().isInstanceOf(IOException.class).hasMessage("Brak pliku z cennikiem");
    }

    @Test
    @DisplayName("6. Zdarzenia startu po kolei; bean z @EventListener widzi je dopiero od ContextRefreshedEvent")
    void lifecycleEvents() {
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        ApplicationListener<ApplicationEvent> listener = event -> events.add(event.getClass().getSimpleName());
        SpringApplication application = quietApplication();
        application.addListeners(listener);   // słuchacz dodany do SpringApplication — istnieje PRZED kontekstem

        StartupJournal journal;
        try (ConfigurableApplicationContext context = application.run("--limit=1")) {
            journal = context.getBean(StartupJournal.class);
            assertThat(events).containsExactly(
                    "ApplicationStartingEvent",
                    "ApplicationEnvironmentPreparedEvent",
                    "ApplicationContextInitializedEvent",
                    "ApplicationPreparedEvent",
                    "ContextRefreshedEvent",
                    "ApplicationStartedEvent",
                    "AvailabilityChangeEvent",
                    "ApplicationReadyEvent",
                    "AvailabilityChangeEvent");
        }
        assertThat(events).endsWith("ContextClosedEvent");

        // bean z @EventListener: brak czterech pierwszych zdarzeń; runnery są między Started a Ready
        assertThat(journal.entries()).containsExactly(
                "utworzono MonthlyReportGenerator",
                "zdarzenie: ContextRefreshedEvent",
                "zdarzenie: ApplicationStartedEvent",
                "zdarzenie: AvailabilityChangeEvent",
                "runner 1 (ApplicationRunner): opcje [limit], bez opcji [], wczytano 1",
                "runner 2 (CommandLineRunner): argumenty [--limit=1], w katalogu 1",
                "zdarzenie: ApplicationReadyEvent",
                "zdarzenie: AvailabilityChangeEvent",
                "zdarzenie: ContextClosedEvent");
    }

    @Test
    @DisplayName("7. Kod wyjścia: SpringApplication.exit zamyka kontekst i zwraca kod z beanów ExitCodeGenerator")
    void exitCode() {
        ConfigurableApplicationContext full = quietApplication().run();
        assertThat(SpringApplication.exit(full)).isZero();
        assertThat(full.isActive()).as("exit zamyka kontekst").isFalse();

        ConfigurableApplicationContext empty = quietApplication().run("--limit=0");
        assertThat(SpringApplication.exit(empty)).isEqualTo(EmptyCatalogExitCode.EMPTY_CATALOG);
        assertThat(empty.isActive()).isFalse();
    }
}
