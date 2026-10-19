package s15_actuator_logging.obs05_observations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s15_actuator_logging.obs05_observations.Obs05Observations.MiniObservation;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Obs05Observations. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Obs05ObservationsExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: które klucze staną się tagami metryki")
    void exercise1() {
        assertThat(Obs05Observations.exercise1(Map.of("carrier", "DHL"), Map.of("orderId", "ZAM-1")))
                .containsExactly("carrier", "error");
        assertThat(Obs05Observations.exercise1(Map.of("method", "GET", "carrier", "DHL"), Map.of()))
                .containsExactly("carrier", "error", "method");
        assertThat(Obs05Observations.exercise1(Map.of(), Map.of("orderId", "ZAM-1"))).containsExactly("error");
    }

    @Test
    @DisplayName("Ćwiczenie 2: obserwacja zapisuje start, ewentualnie error, i zawsze stop")
    void exercise2() {
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
    @DisplayName("Ćwiczenie 3: czy obserwacja jest włączona (najdłuższy pasujący przedrostek)")
    void exercise3() {
        Map<String, Boolean> rules = Map.of("http", false, "http.server", true);

        assertThat(Obs05Observations.exercise3(rules, "http.client.requests")).isFalse();
        assertThat(Obs05Observations.exercise3(rules, "http.server.requests")).isTrue();
        assertThat(Obs05Observations.exercise3(rules, "deliveries.ship")).isTrue();
        assertThat(Obs05Observations.exercise3(rules, "httpx.cos")).isTrue();      // "http" to nie "httpx"
        assertThat(Obs05Observations.exercise3(rules, "http")).isFalse();           // nazwa równa przedrostkowi
        assertThat(Obs05Observations.exercise3(Map.of(), "cokolwiek")).isTrue();
    }
}
