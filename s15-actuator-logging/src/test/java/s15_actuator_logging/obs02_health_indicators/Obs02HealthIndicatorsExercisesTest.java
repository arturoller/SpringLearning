package s15_actuator_logging.obs02_health_indicators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s15_actuator_logging.obs02_health_indicators.Obs02HealthIndicators.HealthResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Obs02HealthIndicators. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Obs02HealthIndicatorsExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: status zdrowia na kod HTTP")
    void exercise1() {
        assertThat(Obs02HealthIndicators.exercise1("UP")).isEqualTo(200);
        assertThat(Obs02HealthIndicators.exercise1("UNKNOWN")).isEqualTo(200);
        assertThat(Obs02HealthIndicators.exercise1("DOWN")).isEqualTo(503);
        assertThat(Obs02HealthIndicators.exercise1("OUT_OF_SERVICE")).isEqualTo(503);
        assertThatThrownBy(() -> Obs02HealthIndicators.exercise1("SLEEPING"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 2: status zbiorczy — wygrywa najgorszy")
    void exercise2() {
        assertThat(Obs02HealthIndicators.exercise2(List.of("UP", "UP"))).isEqualTo("UP");
        assertThat(Obs02HealthIndicators.exercise2(List.of("UP", "UNKNOWN"))).isEqualTo("UP");
        assertThat(Obs02HealthIndicators.exercise2(List.of("UP", "OUT_OF_SERVICE"))).isEqualTo("OUT_OF_SERVICE");
        assertThat(Obs02HealthIndicators.exercise2(List.of("OUT_OF_SERVICE", "DOWN", "UP"))).isEqualTo("DOWN");
        assertThat(Obs02HealthIndicators.exercise2(List.of())).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("Ćwiczenie 3: kontrola wolnego miejsca na dysku")
    void exercise3() {
        assertThat(Obs02HealthIndicators.exercise3(500, 100))
                .isEqualTo(new HealthResult("UP", Map.of("free", 500L, "threshold", 100L)));
        assertThat(Obs02HealthIndicators.exercise3(100, 100).status()).isEqualTo("UP");
        assertThat(Obs02HealthIndicators.exercise3(50, 100))
                .isEqualTo(new HealthResult("DOWN",
                        Map.of("free", 50L, "threshold", 100L, "error", "Za mało miejsca na dysku")));
        assertThatThrownBy(() -> Obs02HealthIndicators.exercise3(-1, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Obs02HealthIndicators.exercise3(10, -100))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
