package s03_boot_basics.boot03_spring_application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Boot03SpringApplication. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Boot03SpringApplicationExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: runner liczy produkty z --limit (najwyżej 14), bez opcji — 14")
    void exercise1() {
        Boot03SpringApplication.ProductCountRunner runner = new Boot03SpringApplication.ProductCountRunner();
        runner.run(new DefaultApplicationArguments("--limit=3", "plik.csv"));
        assertThat(runner.count()).isEqualTo(3);
        runner.run(new DefaultApplicationArguments("--limit=100"));
        assertThat(runner.count()).isEqualTo(14);
        runner.run(new DefaultApplicationArguments("plik.csv"));
        assertThat(runner.count()).isEqualTo(14);
    }

    @Test
    @DisplayName("Ćwiczenie 2: słuchacz z addListeners zapisuje wszystkie zdarzenia — także sprzed utworzenia kontekstu")
    void exercise2() {
        Boot03SpringApplication.EventNamesRecorder recorder = new Boot03SpringApplication.EventNamesRecorder();
        SpringApplication application = new SpringApplication(Boot03Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.addListeners(recorder);
        try (ConfigurableApplicationContext context = application.run()) {
            assertThat(recorder.names()).startsWith("ApplicationStartingEvent", "ApplicationEnvironmentPreparedEvent",
                    "ApplicationContextInitializedEvent", "ApplicationPreparedEvent");
            assertThat(recorder.names()).contains("ApplicationReadyEvent");
        }
        assertThat(recorder.names()).endsWith("ContextClosedEvent");
    }

    @Test
    @DisplayName("Ćwiczenie 3: kod wyjścia raportu importu — 0, 1 albo 2; SpringApplication.exit go zwraca")
    void exercise3() {
        assertThat(new Boot03SpringApplication.ImportReport(10, 0).getExitCode()).isZero();
        assertThat(new Boot03SpringApplication.ImportReport(10, 2).getExitCode()).isEqualTo(1);
        assertThat(new Boot03SpringApplication.ImportReport(0, 5).getExitCode()).isEqualTo(2);
        assertThat(new Boot03SpringApplication.ImportReport(0, 0).getExitCode()).isEqualTo(2);

        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(Boot03SpringApplication.ImportReport.class, () -> new Boot03SpringApplication.ImportReport(7, 1));
        context.refresh();
        assertThat(SpringApplication.exit(context)).isEqualTo(1);
    }
}
