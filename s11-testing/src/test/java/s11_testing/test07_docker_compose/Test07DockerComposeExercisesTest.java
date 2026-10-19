package s11_testing.test07_docker_compose;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.docker.compose.lifecycle.DockerComposeProperties;
import org.springframework.boot.docker.compose.lifecycle.LifecycleManagement;
import org.springframework.boot.docker.compose.lifecycle.StopCommand;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Test07DockerCompose. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Żadne ćwiczenie nie uruchamia Dockera — sprawdzamy ustawienia i plik Compose.
 */
@Tag("cwiczenie")
class Test07DockerComposeExercisesTest {

    /** Znacznik „tu jeszcze nic nie ma” — rzuca wyjątek, dopóki go nie usuniesz. */
    static void todo() {
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 1: usługi startują z aplikacją, ale NIE są zatrzymywane; bez czekania na gotowość")
    void exercise1() {
        var properties = new DockerComposeProperties();

        // TODO: ustaw w obiekcie properties: cykl życia „tylko start” i brak sprawdzania gotowości usług (readiness)
        todo();

        assertThat(properties.getLifecycleManagement()).isEqualTo(LifecycleManagement.START_ONLY);
        assertThat(properties.getReadiness().getWait()).isEqualTo(DockerComposeProperties.Readiness.Wait.NEVER);
    }

    @Test
    @DisplayName("Ćwiczenie 2: właściwości spring.docker.compose.* z mapy wiążą się jak w application.properties")
    void exercise2() {
        // Binder = ten sam mechanizm, którym Boot wiąże application.properties z klasą DockerComposeProperties.
        var binder = new Binder(new MapConfigurationPropertySource(Test07DockerCompose.exercise2()));
        DockerComposeProperties bound = binder.bind("spring.docker.compose", DockerComposeProperties.class).get();

        assertThat(bound.isEnabled()).isTrue();
        assertThat(bound.getFile()).containsExactly(new File("compose/test07/compose.yaml"));
        assertThat(bound.getStop().getCommand()).isEqualTo(StopCommand.DOWN);
        assertThat(bound.getSkip().isInTests()).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 3: plik Compose w compose/exercise07 — standardowa nazwa, jawny tag, port bez portu hosta")
    void exercise3() throws IOException {
        ComposeFileChecks.assertGoodPostgresComposeFile(new File("compose/exercise07"));
    }
}
