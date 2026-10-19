package s19_deploy.solutions.dep04_startup_and_shutdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Dep04StartupAndShutdownExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Dep04StartupAndShutdownSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: czy kierować ruch do instancji")
    void solution1() {
        assertThat(Dep04StartupAndShutdownSolutions.solution1("UP", "UP")).isTrue();
        assertThat(Dep04StartupAndShutdownSolutions.solution1("UP", "OUT_OF_SERVICE")).isFalse();
        assertThat(Dep04StartupAndShutdownSolutions.solution1("DOWN", "UP")).isTrue();
        assertThat(Dep04StartupAndShutdownSolutions.solution1("UP", "DOWN")).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 2: decyzja platformy na podstawie sond")
    void solution2() {
        assertThat(Dep04StartupAndShutdownSolutions.solution2("UP", "UP")).isEqualTo("ZOSTAW");
        assertThat(Dep04StartupAndShutdownSolutions.solution2("UP", "OUT_OF_SERVICE")).isEqualTo("WYLACZ_Z_BALANSERA");
        assertThat(Dep04StartupAndShutdownSolutions.solution2("DOWN", "UP")).isEqualTo("RESTART");
        assertThat(Dep04StartupAndShutdownSolutions.solution2("DOWN", "OUT_OF_SERVICE")).isEqualTo("RESTART");
    }

    @Test
    @DisplayName("Wzorzec 3: sprawdzenie ustawień zamykania")
    void solution3() {
        assertThat(Dep04StartupAndShutdownSolutions.solution3(Duration.ofSeconds(30), 5, 60)).isEmpty();
        assertThat(Dep04StartupAndShutdownSolutions.solution3(Duration.ofSeconds(30), 5, 30)).containsExactly("ZA_KROTKI_CZAS_ZAMKNIECIA");
        assertThat(Dep04StartupAndShutdownSolutions.solution3(Duration.ofSeconds(30), 5, 35)).containsExactly("ZA_KROTKI_CZAS_ZAMKNIECIA");
        assertThat(Dep04StartupAndShutdownSolutions.solution3(Duration.ZERO, 0, 30)).containsExactly("ZEROWY_LIMIT");
        assertThat(Dep04StartupAndShutdownSolutions.solution3(Duration.ofSeconds(-1), 10, 5))
                .containsExactly("ZA_KROTKI_CZAS_ZAMKNIECIA", "ZEROWY_LIMIT");
    }
}
