package s01_core_ioc.ioc01_container;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Ioc01Container. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
class Ioc01ContainerExercisesTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Ćwiczenie 1: suma zamówienia z beana OrderCalculator")
    void exercise1() {
        assertThat(Ioc01Container.exercise1(context)).isEqualTo(7597);
    }

    @Test
    @DisplayName("Ćwiczenie 2: goły kontener Springa")
    void exercise2() {
        assertThat(Ioc01Container.exercise2()).isEqualTo(2697);
    }

    @Test
    @DisplayName("Ćwiczenie 3: nazwy naszych beanów")
    void exercise3() {
        assertThat(Ioc01Container.exercise3(context))
                .containsExactly("ioc01Application", "orderCalculator", "priceList");
    }
}
