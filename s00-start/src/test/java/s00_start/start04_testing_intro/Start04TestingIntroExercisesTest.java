package s00_start.start04_testing_intro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Start04TestingIntro. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Start04TestingIntroExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: dozwolone stawki VAT")
    void exercise1() {
        assertThat(Start04TestingIntro.exercise1(23)).isTrue();
        assertThat(Start04TestingIntro.exercise1(0)).isTrue();
        assertThat(Start04TestingIntro.exercise1(7)).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 2: brutto po rabacie")
    void exercise2() {
        assertThat(Start04TestingIntro.exercise2(10_000, 23, 10)).isEqualTo(11_070);
        assertThat(Start04TestingIntro.exercise2(10_000, 23, 0)).isEqualTo(12_300);
        assertThat(Start04TestingIntro.exercise2(10_000, 23, 100)).isZero();
        assertThatThrownBy(() -> Start04TestingIntro.exercise2(10_000, 23, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 3: suma koszyka z zaokrągleniem każdej pozycji")
    void exercise3() {
        assertThat(Start04TestingIntro.exercise3(List.of(999, 999), 23)).isEqualTo(2458);
        assertThat(Start04TestingIntro.exercise3(List.of(1, 1, 1), 23)).isEqualTo(3);
        assertThat(Start04TestingIntro.exercise3(List.of(), 23)).isZero();
    }
}
