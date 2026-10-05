package s00_start.start01_how_to_use;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Start01HowToUse. @Tag("cwiczenie") — wyłączone z ./mvnw test; uruchamiasz je sam (▶ obok klasy).
 * Czerwony pasek = ćwiczenie jeszcze nierozwiązane (na początku każde rzuca UnsupportedOperationException("TODO")).
 */
@Tag("cwiczenie")
class Start01HowToUseExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: pakiet działu → moduł")
    void exercise1() {
        assertThat(Start01HowToUse.exercise1("s04_web_rest")).isEqualTo("s04-web-rest");
        assertThat(Start01HowToUse.exercise1("s16_cache_scheduling_async")).isEqualTo("s16-cache-scheduling-async");
    }

    @Test
    @DisplayName("Ćwiczenie 2: klasa lekcji → pakiet lekcji")
    void exercise2() {
        assertThat(Start01HowToUse.exercise2("Ioc02ConstructorInjection")).isEqualTo("ioc02_constructor_injection");
        assertThat(Start01HowToUse.exercise2("Start01HowToUse")).isEqualTo("start01_how_to_use");
    }

    @Test
    @DisplayName("Ćwiczenie 3: czy to dzień powtórki")
    void exercise3() {
        LocalDate learned = LocalDate.of(2026, 3, 1);
        assertThat(Start01HowToUse.exercise3(learned, LocalDate.of(2026, 3, 2))).isTrue();
        assertThat(Start01HowToUse.exercise3(learned, LocalDate.of(2026, 3, 31))).isTrue();
        assertThat(Start01HowToUse.exercise3(learned, LocalDate.of(2026, 3, 1))).isFalse();
        assertThat(Start01HowToUse.exercise3(learned, LocalDate.of(2026, 3, 5))).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 4: ścieżka pliku testu lekcji")
    void exercise4() {
        assertThat(Start01HowToUse.exercise4("s04-web-rest", "Rest01Hello"))
                .isEqualTo("s04-web-rest/src/test/java/s04_web_rest/rest01_hello/Rest01HelloTest.java");
    }
}
