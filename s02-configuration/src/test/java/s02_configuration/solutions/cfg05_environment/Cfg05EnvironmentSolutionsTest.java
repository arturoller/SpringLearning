package s02_configuration.solutions.cfg05_environment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Cfg05EnvironmentExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Wystarczą MockEnvironment i lekki ApplicationContextRunner — bez klasy startowej.
 */
@Tag("wzorzec")
class Cfg05EnvironmentSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: liczba prób z domyślną wartością 3")
    void solution1() {
        assertThat(Cfg05EnvironmentSolutions.solution1(new MockEnvironment())).isEqualTo(3);
        assertThat(Cfg05EnvironmentSolutions.solution1(new MockEnvironment().withProperty("cfg05.exercise.retries", "7")))
                .isEqualTo(7);
    }

    @Test
    @DisplayName("Wzorzec 2: ExpressShipping istnieje tylko przy cfg05.exercise.express=true")
    void solution2() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(Cfg05EnvironmentSolutions.ExpressShipping.class);
        runner.run(context -> assertThat(context).doesNotHaveBean(Cfg05EnvironmentSolutions.ExpressShipping.class));
        runner.withPropertyValues("cfg05.exercise.express=false")
                .run(context -> assertThat(context).doesNotHaveBean(Cfg05EnvironmentSolutions.ExpressShipping.class));
        runner.withPropertyValues("cfg05.exercise.express=true")
                .run(context -> assertThat(context).hasSingleBean(Cfg05EnvironmentSolutions.ExpressShipping.class));
    }

    @Test
    @DisplayName("Wzorzec 3: wartość klucza z podstawionej zmiennej środowiska — działają oba zapisy nazwy")
    void solution3() {
        assertThat(Cfg05EnvironmentSolutions.solution3(Map.of("CFG05_EXERCISE_MAXDAYS", "4"))).isEqualTo(4);
        assertThat(Cfg05EnvironmentSolutions.solution3(Map.of("CFG05_EXERCISE_MAX_DAYS", "9"))).isEqualTo(9);
        assertThat(Cfg05EnvironmentSolutions.solution3(Map.of("INNA_ZMIENNA", "1"))).isEqualTo(-1);
    }
}
