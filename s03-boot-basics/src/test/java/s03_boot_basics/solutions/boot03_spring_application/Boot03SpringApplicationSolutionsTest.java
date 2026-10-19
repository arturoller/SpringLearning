package s03_boot_basics.solutions.boot03_spring_application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import s03_boot_basics.boot03_spring_application.Boot03Application;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Boot03SpringApplicationExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Boot03SpringApplicationSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: runner liczy produkty z --limit (najwyżej 14), bez opcji — 14")
    void solution1() {
        Boot03SpringApplicationSolutions.ProductCountRunner runner = new Boot03SpringApplicationSolutions.ProductCountRunner();
        runner.run(new DefaultApplicationArguments("--limit=3", "plik.csv"));
        assertThat(runner.count()).isEqualTo(3);
        runner.run(new DefaultApplicationArguments("--limit=100"));
        assertThat(runner.count()).isEqualTo(14);
        runner.run(new DefaultApplicationArguments("plik.csv"));
        assertThat(runner.count()).isEqualTo(14);
    }

    @Test
    @DisplayName("Wzorzec 2: słuchacz z addListeners zapisuje wszystkie zdarzenia — także sprzed utworzenia kontekstu")
    void solution2() {
        Boot03SpringApplicationSolutions.EventNamesRecorder recorder = new Boot03SpringApplicationSolutions.EventNamesRecorder();
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
    @DisplayName("Wzorzec 3: kod wyjścia raportu importu — 0, 1 albo 2; SpringApplication.exit go zwraca")
    void solution3() {
        assertThat(new Boot03SpringApplicationSolutions.ImportReport(10, 0).getExitCode()).isZero();
        assertThat(new Boot03SpringApplicationSolutions.ImportReport(10, 2).getExitCode()).isEqualTo(1);
        assertThat(new Boot03SpringApplicationSolutions.ImportReport(0, 5).getExitCode()).isEqualTo(2);
        assertThat(new Boot03SpringApplicationSolutions.ImportReport(0, 0).getExitCode()).isEqualTo(2);

        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(Boot03SpringApplicationSolutions.ImportReport.class,
                () -> new Boot03SpringApplicationSolutions.ImportReport(7, 1));
        context.refresh();
        assertThat(SpringApplication.exit(context)).isEqualTo(1);
    }
}
