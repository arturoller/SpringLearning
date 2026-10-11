package s01_core_ioc.ioc02_constructor_injection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Ioc02ConstructorInjection. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Ioc02ConstructorInjectionExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: serwis złożony ręcznie")
    void exercise1() {
        assertThat(Ioc02ConstructorInjection.exercise1(2)).isEqualTo("Jan Nowak");
        assertThatThrownBy(() -> Ioc02ConstructorInjection.exercise1(99))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 2: dubler repozytorium jako lambda")
    void exercise2() {
        assertThat(Ioc02ConstructorInjection.exercise2()).isEqualTo("Test Testowy");
    }

    @Test
    @DisplayName("Ćwiczenie 3: goły kontener Springa")
    void exercise3() {
        assertThat(Ioc02ConstructorInjection.exercise3(3)).isEqualTo("Maria Wiśniewska");
    }
}
