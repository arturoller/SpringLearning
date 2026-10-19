package s19_deploy.dep04_startup_and_shutdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Dep04StartupAndShutdown. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Dep04StartupAndShutdownExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: czy kierować ruch do instancji (decyduje readiness)")
    void exercise1() {
        assertThat(Dep04StartupAndShutdown.exercise1("UP", "UP")).isTrue();
        assertThat(Dep04StartupAndShutdown.exercise1("UP", "OUT_OF_SERVICE")).isFalse();
        assertThat(Dep04StartupAndShutdown.exercise1("DOWN", "UP")).isTrue();
        assertThat(Dep04StartupAndShutdown.exercise1("UP", "DOWN")).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 2: decyzja platformy na podstawie sond")
    void exercise2() {
        assertThat(Dep04StartupAndShutdown.exercise2("UP", "UP")).isEqualTo("ZOSTAW");
        assertThat(Dep04StartupAndShutdown.exercise2("UP", "OUT_OF_SERVICE")).isEqualTo("WYLACZ_Z_BALANSERA");
        assertThat(Dep04StartupAndShutdown.exercise2("DOWN", "UP")).isEqualTo("RESTART");
        assertThat(Dep04StartupAndShutdown.exercise2("DOWN", "OUT_OF_SERVICE")).isEqualTo("RESTART");
    }

    @Test
    @DisplayName("Ćwiczenie 3: sprawdzenie ustawień zamykania")
    void exercise3() {
        assertThat(Dep04StartupAndShutdown.exercise3(Duration.ofSeconds(30), 5, 60)).isEmpty();
        assertThat(Dep04StartupAndShutdown.exercise3(Duration.ofSeconds(30), 5, 30)).containsExactly("ZA_KROTKI_CZAS_ZAMKNIECIA");
        assertThat(Dep04StartupAndShutdown.exercise3(Duration.ofSeconds(30), 5, 35)).containsExactly("ZA_KROTKI_CZAS_ZAMKNIECIA");
        assertThat(Dep04StartupAndShutdown.exercise3(Duration.ZERO, 0, 30)).containsExactly("ZEROWY_LIMIT");
        assertThat(Dep04StartupAndShutdown.exercise3(Duration.ofSeconds(-1), 10, 5))
                .containsExactly("ZA_KROTKI_CZAS_ZAMKNIECIA", "ZEROWY_LIMIT");
    }
}
