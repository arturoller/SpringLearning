package s03_boot_basics.solutions.boot03_spring_application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import s03_boot_basics.boot03_spring_application.Boot03Application;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Boot03SpringApplicationExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Boot03SpringApplicationSolutionsTest {

    private static SpringApplication plainApplication() {
        SpringApplication application = new SpringApplication(Boot03Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        return application;
    }

    @Test
    @DisplayName("Wzorzec 1: SpringApplication z typem NONE, profilem demo i ustawieniem domyślnym")
    void solution1() {
        SpringApplication application = Boot03SpringApplicationSolutions.solution1Application("sklepowy");
        assertThat(application.getWebApplicationType()).isEqualTo(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = application.run()) {
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("demo");
            assertThat(context.getEnvironment().getProperty("shop.mode")).isEqualTo("sklepowy");
        }
        // argument programu przesłania wartość domyślną
        try (ConfigurableApplicationContext context = Boot03SpringApplicationSolutions.solution1Application("sklepowy").run("--shop.mode=argument")) {
            assertThat(context.getEnvironment().getProperty("shop.mode")).isEqualTo("argument");
        }
    }

    @Test
    @DisplayName("Wzorzec 2: opis argumentów programu")
    void solution2() {
        assertThat(Boot03SpringApplicationSolutions.solution2DescribeArguments(
                new DefaultApplicationArguments("--x=1", "--x=2", "--a=z", "foo", "bar")))
                .isEqualTo("a=z x=1,2 | foo bar");
        assertThat(Boot03SpringApplicationSolutions.solution2DescribeArguments(
                new DefaultApplicationArguments("--verbose", "plik")))
                .isEqualTo("verbose= | plik");
        assertThat(Boot03SpringApplicationSolutions.solution2DescribeArguments(
                new DefaultApplicationArguments()))
                .isEqualTo(" | ");
    }

    @Test
    @DisplayName("Wzorzec 3: odbiornik zapisuje tylko zdarzenia startu Boota, w kolejności")
    void solution3() {
        List<String> sink = new ArrayList<>();
        SpringApplication application = plainApplication();
        application.addListeners(Boot03SpringApplicationSolutions.solution3BootEventRecorder(sink));
        try (ConfigurableApplicationContext context = application.run()) {
            assertThat(sink).containsExactly(
                    "ApplicationStartingEvent",
                    "ApplicationEnvironmentPreparedEvent",
                    "ApplicationContextInitializedEvent",
                    "ApplicationPreparedEvent",
                    "ApplicationStartedEvent",
                    "ApplicationReadyEvent");
        }
    }

    @Test
    @DisplayName("Wzorzec 4: kod wyjścia 0 albo 10 + liczba nieudanych zamówień, a kontekst zamknięty")
    void solution4() {
        ConfigurableApplicationContext ok = plainApplication().run();
        assertThat(Boot03SpringApplicationSolutions.solution4ExitCode(ok, 0)).isZero();
        assertThat(ok.isActive()).isFalse();

        ConfigurableApplicationContext failed = plainApplication().run();
        assertThat(Boot03SpringApplicationSolutions.solution4ExitCode(failed, 3)).isEqualTo(13);
        assertThat(failed.isActive()).isFalse();
    }
}
