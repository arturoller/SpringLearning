package s15_actuator_logging.solutions.obs01_actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Obs01ActuatorExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Obs01ActuatorSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: linia właściwości z wystawionymi punktami końcowymi")
    void solution1() {
        assertThat(Obs01ActuatorSolutions.solution1(List.of("health", "info")))
                .isEqualTo("management.endpoints.web.exposure.include=health,info");
        assertThatThrownBy(() -> Obs01ActuatorSolutions.solution1(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 2: czy punkt końcowy jest wystawiony (include, exclude, gwiazdka)")
    void solution2() {
        assertThat(Obs01ActuatorSolutions.solution2("health,info", "", "info")).isTrue();
        assertThat(Obs01ActuatorSolutions.solution2("*", "env", "env")).isFalse();
        assertThat(Obs01ActuatorSolutions.solution2("*", "env", "metrics")).isTrue();
        assertThat(Obs01ActuatorSolutions.solution2("health", "", "env")).isFalse();
        assertThat(Obs01ActuatorSolutions.solution2(" Health , INFO ", "", "info")).isTrue();
        assertThat(Obs01ActuatorSolutions.solution2("", "", "health")).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 3: ryzykowne punkty końcowe posortowane alfabetycznie")
    void solution3() {
        assertThat(Obs01ActuatorSolutions.solution3(Set.of("health", "env", "metrics", "beans")))
                .containsExactly("beans", "env");
        assertThat(Obs01ActuatorSolutions.solution3(Set.of("health", "info"))).isEmpty();
    }
}
