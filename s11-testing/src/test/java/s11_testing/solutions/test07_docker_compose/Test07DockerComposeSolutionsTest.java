package s11_testing.solutions.test07_docker_compose;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.docker.compose.lifecycle.DockerComposeProperties;
import org.springframework.boot.docker.compose.lifecycle.LifecycleManagement;
import org.springframework.boot.docker.compose.lifecycle.StopCommand;
import s11_testing.test07_docker_compose.ComposeFileChecks;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Test07DockerCompose (te same sprawdzenia co w Test07DockerComposeExercisesTest).
 * {@code @Tag("wzorzec")} — biegną zawsze. Zajrzyj dopiero po własnej próbie!
 */
@Tag("wzorzec")
class Test07DockerComposeSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: usługi startują z aplikacją, ale NIE są zatrzymywane; bez czekania na gotowość")
    void solution1() {
        var properties = new DockerComposeProperties();

        properties.setLifecycleManagement(LifecycleManagement.START_ONLY);          // ...lifecycle-management=start-only
        properties.getReadiness().setWait(DockerComposeProperties.Readiness.Wait.NEVER);   // ...readiness.wait=never

        assertThat(properties.getLifecycleManagement()).isEqualTo(LifecycleManagement.START_ONLY);
        assertThat(properties.getReadiness().getWait()).isEqualTo(DockerComposeProperties.Readiness.Wait.NEVER);
    }

    @Test
    @DisplayName("Wzorzec 2: właściwości spring.docker.compose.* z mapy wiążą się jak w application.properties")
    void solution2() {
        var binder = new Binder(new MapConfigurationPropertySource(Test07DockerComposeSolutions.solution2()));
        DockerComposeProperties bound = binder.bind("spring.docker.compose", DockerComposeProperties.class).get();

        assertThat(bound.isEnabled()).isTrue();
        assertThat(bound.getFile()).containsExactly(new File("compose/test07/compose.yaml"));
        assertThat(bound.getStop().getCommand()).isEqualTo(StopCommand.DOWN);
        assertThat(bound.getSkip().isInTests()).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 3: plik Compose — standardowa nazwa, jawny tag, port bez portu hosta")
    void solution3() throws IOException {
        ComposeFileChecks.assertGoodPostgresComposeFile(new File("compose/solutions/exercise07"));
    }
}
