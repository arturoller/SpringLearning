package s15_actuator_logging.solutions.obs02_health_indicators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s15_actuator_logging.obs02_health_indicators.Obs02HealthIndicators.HealthResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Obs02HealthIndicatorsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Obs02HealthIndicatorsSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: status zdrowia na kod HTTP")
    void solution1() {
        assertThat(Obs02HealthIndicatorsSolutions.solution1("UP")).isEqualTo(200);
        assertThat(Obs02HealthIndicatorsSolutions.solution1("UNKNOWN")).isEqualTo(200);
        assertThat(Obs02HealthIndicatorsSolutions.solution1("DOWN")).isEqualTo(503);
        assertThat(Obs02HealthIndicatorsSolutions.solution1("OUT_OF_SERVICE")).isEqualTo(503);
        assertThatThrownBy(() -> Obs02HealthIndicatorsSolutions.solution1("SLEEPING"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 2: status zbiorczy — wygrywa najgorszy")
    void solution2() {
        assertThat(Obs02HealthIndicatorsSolutions.solution2(List.of("UP", "UP"))).isEqualTo("UP");
        assertThat(Obs02HealthIndicatorsSolutions.solution2(List.of("UP", "UNKNOWN"))).isEqualTo("UP");
        assertThat(Obs02HealthIndicatorsSolutions.solution2(List.of("UP", "OUT_OF_SERVICE"))).isEqualTo("OUT_OF_SERVICE");
        assertThat(Obs02HealthIndicatorsSolutions.solution2(List.of("OUT_OF_SERVICE", "DOWN", "UP"))).isEqualTo("DOWN");
        assertThat(Obs02HealthIndicatorsSolutions.solution2(List.of())).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("Wzorzec 3: kontrola wolnego miejsca na dysku")
    void solution3() {
        assertThat(Obs02HealthIndicatorsSolutions.solution3(500, 100))
                .isEqualTo(new HealthResult("UP", Map.of("free", 500L, "threshold", 100L)));
        assertThat(Obs02HealthIndicatorsSolutions.solution3(100, 100).status()).isEqualTo("UP");
        assertThat(Obs02HealthIndicatorsSolutions.solution3(50, 100))
                .isEqualTo(new HealthResult("DOWN",
                        Map.of("free", 50L, "threshold", 100L, "error", "Za mało miejsca na dysku")));
        assertThatThrownBy(() -> Obs02HealthIndicatorsSolutions.solution3(-1, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Obs02HealthIndicatorsSolutions.solution3(10, -100))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
