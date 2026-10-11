package s03_boot_basics.boot03_spring_application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Boot03SpringApplication. Bez {@code @SpringBootTest}: tematem lekcji jest SAMO uruchamianie, więc testy
 * wołają {@code Boot03Application.createApplication().run(...)} jak metoda main i zamykają kontekst (try-with-resources).
 * OutputCaptureExtension (przechwytywanie wyjścia) — narzędzie Boota do sprawdzania, co trafiło na konsolę.
 */
@ExtendWith(OutputCaptureExtension.class)
class Boot03SpringApplicationTest {

    @Test
    @DisplayName("1. Ustawienia SpringApplication: brak weba, brak bannera, profil boot03, właściwości domyślne")
    void springApplicationSettings(CapturedOutput output) {
        try (ConfigurableApplicationContext context = Boot03Application.createApplication().run()) {
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("boot03");
            assertThat(context.getEnvironment().getProperty("boot03.profile-message")).isEqualTo("Profil boot03 aktywny");
            assertThat(context.getEnvironment().getProperty("boot03.shop-name")).isEqualTo("Sklep Domyślny");
        }
        assertThat(output).doesNotContain(":: Spring Boot ::");

        // dla porównania: domyślna aplikacja wypisuje banner, a typ aplikacji wykrywa z classpath (tu: NONE)
        SpringApplication plain = new SpringApplication(Boot03Application.class);
        assertThat(plain.getWebApplicationType()).isEqualTo(WebApplicationType.NONE);
        plain.run().close();
        assertThat(output).contains(":: Spring Boot ::");
    }

    @Test
    @DisplayName("1. Argument --boot03.shop-name wygrywa z właściwością domyślną (pytanie 2)")
    void commandLineBeatsDefaultProperties() {
        try (var context = Boot03Application.createApplication().run("--boot03.shop-name=Sklep z Linii Poleceń")) {
            assertThat(context.getEnvironment().getProperty("boot03.shop-name")).isEqualTo("Sklep z Linii Poleceń");
        }
    }

    @Test
    @DisplayName("2. ApplicationRunner widzi opcje i słowa osobno, CommandLineRunner — surowe argumenty; @Order ustala kolejność")
    void runnersAndArguments() {
        try (var context = Boot03Application.createApplication().run("--import.file=ceny.csv", "pilne")) {
            assertThat(context.getBean(RunLog.class).entries()).containsSubsequence(
                    "ApplicationRunner: plik=ceny.csv, słowa=[pilne]",
                    "CommandLineRunner: argumenty=[--import.file=ceny.csv, pilne]");
            // opcja z linii poleceń jest też zwykłą właściwością środowiska:
            assertThat(context.getEnvironment().getProperty("import.file")).isEqualTo("ceny.csv");
        }
        // pytanie 5: brak opcji → getOptionValues zwraca null, a nie pustą listę
        ApplicationArguments withoutOption = new DefaultApplicationArguments("pilne");
        assertThat(withoutOption.getOptionValues("import.file")).isNull();
        assertThatThrownBy(() -> withoutOption.getOptionValues("import.file").getFirst())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("3. SpringApplication.exit zwraca kod z beana ExitCodeGenerator (domyślnie 0)")
    void exitCode() {
        ConfigurableApplicationContext ok = Boot03Application.createApplication().run();
        assertThat(SpringApplication.exit(ok)).isZero();
        assertThat(ok.isActive()).as("exit zamyka kontekst").isFalse();

        ConfigurableApplicationContext failed = Boot03Application.createApplication().run("--boot03.exit-code=3");
        assertThat(SpringApplication.exit(failed)).isEqualTo(3);
    }

    @Test
    @DisplayName("4. Kolejność startu: ContextRefreshed → ApplicationStarted → runnery → ApplicationReady")
    void startupSequence() {
        try (var context = Boot03Application.createApplication().run()) {
            assertThat(context.getBean(RunLog.class).entries()).containsExactly(
                    "ContextRefreshedEvent",
                    "ApplicationStartedEvent",
                    "ApplicationRunner: plik=brak, słowa=[]",
                    "CommandLineRunner: argumenty=[]",
                    "ApplicationReadyEvent");
        }
    }

    @Test
    @DisplayName("4. Wyjątek w runnerze przerywa start: ApplicationFailedEvent zamiast ApplicationReadyEvent (pytanie 4)")
    void failingRunnerStopsStartup() {
        List<String> events = new ArrayList<>();
        SpringApplicationBuilder builder = new SpringApplicationBuilder(Boot03Application.class, Boot03FailingRunnerConfig.class)
                .web(WebApplicationType.NONE)
                // nasłuchiwacz dodany do SpringApplication widzi też zdarzenia sprzed powstania kontekstu
                .listeners((ApplicationListener<ApplicationEvent>) event -> {
                    if (event instanceof SpringApplicationEvent) {
                        events.add(event.getClass().getSimpleName());
                    }
                });
        assertThatThrownBy(() -> builder.run().close())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to execute CommandLineRunner")
                .hasRootCauseMessage("Plik cennika jest uszkodzony");
        assertThat(events).first().isEqualTo("ApplicationStartingEvent");
        assertThat(events).contains("ApplicationFailedEvent").doesNotContain("ApplicationReadyEvent");

        // wyjątek NIEkontrolowany (RuntimeException) wylatuje z run(...) bez opakowania
        SpringApplicationBuilder withRuntimeFailure = new SpringApplicationBuilder(Boot03Application.class)
                .web(WebApplicationType.NONE)
                .initializers(ctx -> ctx.getBeanFactory().registerSingleton("brokenRunner",
                        (CommandLineRunner) args -> {
                            throw new IllegalArgumentException("Zły format ceny");
                        }));
        assertThatThrownBy(() -> withRuntimeFailure.run().close())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Zły format ceny");
    }

    @Test
    @DisplayName("5. DevTools nie ma na classpath tego modułu (tylko teoria)")
    void devToolsNotOnClasspath() {
        assertThat(ClassUtils.isPresent("org.springframework.boot.devtools.restart.Restarter", getClass().getClassLoader()))
                .isFalse();
    }
}
