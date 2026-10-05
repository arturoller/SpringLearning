package s00_start.solutions.start04_testing_intro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Start04TestingIntroExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Start04TestingIntroSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: dozwolone stawki VAT")
    void solution1() {
        assertThat(Start04TestingIntroSolutions.solution1(23)).isTrue();
        assertThat(Start04TestingIntroSolutions.solution1(0)).isTrue();
        assertThat(Start04TestingIntroSolutions.solution1(7)).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 2: brutto po rabacie")
    void solution2() {
        assertThat(Start04TestingIntroSolutions.solution2(10_000, 23, 10)).isEqualTo(11_070);
        assertThat(Start04TestingIntroSolutions.solution2(10_000, 23, 0)).isEqualTo(12_300);
        assertThat(Start04TestingIntroSolutions.solution2(10_000, 23, 100)).isZero();
        assertThatThrownBy(() -> Start04TestingIntroSolutions.solution2(10_000, 23, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 3: suma koszyka z zaokrągleniem każdej pozycji")
    void solution3() {
        assertThat(Start04TestingIntroSolutions.solution3(List.of(999, 999), 23)).isEqualTo(2458);
        assertThat(Start04TestingIntroSolutions.solution3(List.of(1, 1, 1), 23)).isEqualTo(3);
        assertThat(Start04TestingIntroSolutions.solution3(List.of(), 23)).isZero();
    }
}
