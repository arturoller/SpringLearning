package s03_boot_basics.boot03_spring_application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Boot03SpringApplication. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Boot03SpringApplicationExercisesTest {

    private static SpringApplication plainApplication() {
        SpringApplication application = new SpringApplication(Boot03Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        return application;
    }

    @Test
    @DisplayName("Ćwiczenie 1: SpringApplication z typem NONE, profilem demo i ustawieniem domyślnym")
    void exercise1() {
        SpringApplication application = Boot03SpringApplication.exercise1Application("sklepowy");
        assertThat(application.getWebApplicationType()).isEqualTo(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = application.run()) {
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("demo");
            assertThat(context.getEnvironment().getProperty("shop.mode")).isEqualTo("sklepowy");
        }
        // argument programu przesłania wartość domyślną
        try (ConfigurableApplicationContext context = Boot03SpringApplication.exercise1Application("sklepowy").run("--shop.mode=argument")) {
            assertThat(context.getEnvironment().getProperty("shop.mode")).isEqualTo("argument");
        }
    }

    @Test
    @DisplayName("Ćwiczenie 2: opis argumentów programu")
    void exercise2() {
        assertThat(Boot03SpringApplication.exercise2DescribeArguments(
                new DefaultApplicationArguments("--x=1", "--x=2", "--a=z", "foo", "bar")))
                .isEqualTo("a=z x=1,2 | foo bar");
        assertThat(Boot03SpringApplication.exercise2DescribeArguments(
                new DefaultApplicationArguments("--verbose", "plik")))
                .isEqualTo("verbose= | plik");
        assertThat(Boot03SpringApplication.exercise2DescribeArguments(
                new DefaultApplicationArguments()))
                .isEqualTo(" | ");
    }

    @Test
    @DisplayName("Ćwiczenie 3: odbiornik zapisuje tylko zdarzenia startu Boota, w kolejności")
    void exercise3() {
        List<String> sink = new ArrayList<>();
        SpringApplication application = plainApplication();
        application.addListeners(Boot03SpringApplication.exercise3BootEventRecorder(sink));
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
    @DisplayName("Ćwiczenie 4: kod wyjścia 0 albo 10 + liczba nieudanych zamówień, a kontekst zamknięty")
    void exercise4() {
        ConfigurableApplicationContext ok = plainApplication().run();
        assertThat(Boot03SpringApplication.exercise4ExitCode(ok, 0)).isZero();
        assertThat(ok.isActive()).isFalse();

        ConfigurableApplicationContext failed = plainApplication().run();
        assertThat(Boot03SpringApplication.exercise4ExitCode(failed, 3)).isEqualTo(13);
        assertThat(failed.isActive()).isFalse();
    }
}
