package s15_actuator_logging.solutions.obs05_observations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s15_actuator_logging.solutions.obs05_observations.Obs05ObservationsSolutions.MiniObservation;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Obs05ObservationsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Obs05ObservationsSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: które klucze staną się tagami metryki")
    void solution1() {
        assertThat(Obs05ObservationsSolutions.solution1(Map.of("carrier", "DHL"), Map.of("orderId", "ZAM-1")))
                .containsExactly("carrier", "error");
        assertThat(Obs05ObservationsSolutions.solution1(Map.of("method", "GET", "carrier", "DHL"), Map.of()))
                .containsExactly("carrier", "error", "method");
        assertThat(Obs05ObservationsSolutions.solution1(Map.of(), Map.of("orderId", "ZAM-1")))
                .containsExactly("error");
    }

    @Test
    @DisplayName("Wzorzec 2: obserwacja zapisuje start, ewentualnie error, i zawsze stop")
    void solution2() {
        MiniObservation observation = new MiniObservation();

        assertThat(observation.<String>observe("ok", () -> "wynik")).isEqualTo("wynik");
        assertThatThrownBy(() -> observation.observe("zle", () -> {
            throw new IllegalStateException("awaria");
        })).isInstanceOf(IllegalStateException.class).hasMessage("awaria");

        assertThat(observation.events()).containsExactly(
                "start:ok", "stop:ok",
                "start:zle", "error:zle:IllegalStateException", "stop:zle");
    }

    @Test
    @DisplayName("Wzorzec 3: czy obserwacja jest włączona (najdłuższy pasujący przedrostek)")
    void solution3() {
        Map<String, Boolean> rules = Map.of("http", false, "http.server", true);

        assertThat(Obs05ObservationsSolutions.solution3(rules, "http.client.requests")).isFalse();
        assertThat(Obs05ObservationsSolutions.solution3(rules, "http.server.requests")).isTrue();
        assertThat(Obs05ObservationsSolutions.solution3(rules, "deliveries.ship")).isTrue();
        assertThat(Obs05ObservationsSolutions.solution3(rules, "httpx.cos")).isTrue();
        assertThat(Obs05ObservationsSolutions.solution3(rules, "http")).isFalse();
        assertThat(Obs05ObservationsSolutions.solution3(Map.of(), "cokolwiek")).isTrue();
    }
}
