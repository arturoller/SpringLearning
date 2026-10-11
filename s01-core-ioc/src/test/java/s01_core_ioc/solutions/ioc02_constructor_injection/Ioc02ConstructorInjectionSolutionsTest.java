package s01_core_ioc.solutions.ioc02_constructor_injection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Ioc02ConstructorInjectionExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Ioc02ConstructorInjectionSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: serwis złożony ręcznie")
    void solution1() {
        assertThat(Ioc02ConstructorInjectionSolutions.solution1(2)).isEqualTo("Jan Nowak");
        assertThatThrownBy(() -> Ioc02ConstructorInjectionSolutions.solution1(99))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 2: dubler repozytorium jako lambda")
    void solution2() {
        assertThat(Ioc02ConstructorInjectionSolutions.solution2()).isEqualTo("Test Testowy");
    }

    @Test
    @DisplayName("Wzorzec 3: goły kontener Springa")
    void solution3() {
        assertThat(Ioc02ConstructorInjectionSolutions.solution3(3)).isEqualTo("Maria Wiśniewska");
    }
}
