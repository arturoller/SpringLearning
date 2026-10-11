package s01_core_ioc.solutions.ioc01_container;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import s01_core_ioc.ioc01_container.Ioc01Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Ioc01ContainerExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma własnej klasy startowej, więc wskazujemy ją wprost: {@code classes = Ioc01Application.class}.
 */
@Tag("wzorzec")
@SpringBootTest(classes = Ioc01Application.class)
class Ioc01ContainerSolutionsTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Wzorzec 1: suma zamówienia z beana OrderCalculator")
    void solution1() {
        assertThat(Ioc01ContainerSolutions.solution1(context)).isEqualTo(7597);
    }

    @Test
    @DisplayName("Wzorzec 2: goły kontener Springa")
    void solution2() {
        assertThat(Ioc01ContainerSolutions.solution2()).isEqualTo(2697);
    }

    @Test
    @DisplayName("Wzorzec 3: nazwy naszych beanów")
    void solution3() {
        assertThat(Ioc01ContainerSolutions.solution3(context))
                .containsExactly("ioc01Application", "orderCalculator", "priceList");
    }
}
