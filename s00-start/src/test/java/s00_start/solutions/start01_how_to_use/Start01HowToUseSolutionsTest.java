package s00_start.solutions.start01_how_to_use;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Te same sprawdzenia co w Start01HowToUseExercisesTest, ale na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Start01HowToUseSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: pakiet działu → moduł")
    void solution1() {
        assertThat(Start01HowToUseSolutions.solution1("s04_web_rest")).isEqualTo("s04-web-rest");
        assertThat(Start01HowToUseSolutions.solution1("s16_cache_scheduling_async")).isEqualTo("s16-cache-scheduling-async");
    }

    @Test
    @DisplayName("Wzorzec 2: klasa lekcji → pakiet lekcji")
    void solution2() {
        assertThat(Start01HowToUseSolutions.solution2("Ioc02ConstructorInjection")).isEqualTo("ioc02_constructor_injection");
        assertThat(Start01HowToUseSolutions.solution2("Start01HowToUse")).isEqualTo("start01_how_to_use");
    }

    @Test
    @DisplayName("Wzorzec 3: czy to dzień powtórki")
    void solution3() {
        LocalDate learned = LocalDate.of(2026, 3, 1);
        assertThat(Start01HowToUseSolutions.solution3(learned, LocalDate.of(2026, 3, 2))).isTrue();
        assertThat(Start01HowToUseSolutions.solution3(learned, LocalDate.of(2026, 3, 31))).isTrue();
        assertThat(Start01HowToUseSolutions.solution3(learned, LocalDate.of(2026, 3, 1))).isFalse();
        assertThat(Start01HowToUseSolutions.solution3(learned, LocalDate.of(2026, 3, 5))).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 4: ścieżka pliku testu lekcji")
    void solution4() {
        assertThat(Start01HowToUseSolutions.solution4("s04-web-rest", "Rest01Hello"))
                .isEqualTo("s04-web-rest/src/test/java/s04_web_rest/rest01_hello/Rest01HelloTest.java");
    }
}
