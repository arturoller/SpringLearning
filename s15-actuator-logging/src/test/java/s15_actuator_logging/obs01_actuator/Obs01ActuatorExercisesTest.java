package s15_actuator_logging.obs01_actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Obs01Actuator. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Obs01ActuatorExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: linia właściwości z wystawionymi punktami końcowymi")
    void exercise1() {
        assertThat(Obs01Actuator.exercise1(List.of("health", "info")))
                .isEqualTo("management.endpoints.web.exposure.include=health,info");
        assertThatThrownBy(() -> Obs01Actuator.exercise1(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 2: czy punkt końcowy jest wystawiony (include, exclude, gwiazdka)")
    void exercise2() {
        assertThat(Obs01Actuator.exercise2("health,info", "", "info")).isTrue();
        assertThat(Obs01Actuator.exercise2("*", "env", "env")).isFalse();
        assertThat(Obs01Actuator.exercise2("*", "env", "metrics")).isTrue();
        assertThat(Obs01Actuator.exercise2("health", "", "env")).isFalse();
        assertThat(Obs01Actuator.exercise2(" Health , INFO ", "", "info")).isTrue();
        assertThat(Obs01Actuator.exercise2("", "", "health")).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 3: ryzykowne punkty końcowe posortowane alfabetycznie")
    void exercise3() {
        assertThat(Obs01Actuator.exercise3(Set.of("health", "env", "metrics", "beans")))
                .containsExactly("beans", "env");
        assertThat(Obs01Actuator.exercise3(Set.of("health", "info"))).isEmpty();
    }
}
