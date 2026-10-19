package s11_testing.test07_docker_compose;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.docker.compose.lifecycle.DockerComposeProperties;
import org.springframework.boot.docker.compose.lifecycle.LifecycleManagement;
import org.springframework.boot.docker.compose.lifecycle.StartCommand;
import org.springframework.boot.docker.compose.lifecycle.StopCommand;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 3 lekcji Test07DockerCompose: wartości DOMYŚLNE właściwości {@code spring.docker.compose.*} — odczytane
 * z prawdziwej klasy Boota, do której Spring wiąże te właściwości (DockerComposeProperties). Zwykły test jednostkowy.
 */
class Test07DockerComposeDefaultsTest {

    @Test
    @DisplayName("3. Domyślnie: obsługa włączona, start-and-stop, „up” przy starcie, „stop” przy zamknięciu, pomijanie w testach")
    void defaults() {
        var properties = new DockerComposeProperties();

        assertThat(properties.isEnabled()).isTrue();                                            // spring.docker.compose.enabled
        assertThat(properties.getLifecycleManagement()).isEqualTo(LifecycleManagement.START_AND_STOP);
        assertThat(properties.getStart().getCommand()).isEqualTo(StartCommand.UP);             // docker compose up
        assertThat(properties.getStart().getSkip()).isEqualTo(DockerComposeProperties.Start.Skip.IF_RUNNING);
        assertThat(properties.getStop().getCommand()).isEqualTo(StopCommand.STOP);             // docker compose stop
        assertThat(properties.getStop().getTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.getSkip().isInTests()).isTrue();                                   // spring.docker.compose.skip.in-tests
        assertThat(properties.getReadiness().getWait()).isEqualTo(DockerComposeProperties.Readiness.Wait.ALWAYS);
        assertThat(properties.getReadiness().getTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.getFile()).as("bez wskazania pliku Boot szuka go w katalogu roboczym").isEmpty();
    }
}
