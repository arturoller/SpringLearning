package s02_configuration.cfg05_environment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Cfg05Environment. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Cfg05EnvironmentExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: liczba prób z domyślną wartością 3")
    void exercise1() {
        assertThat(Cfg05Environment.exercise1(new MockEnvironment())).isEqualTo(3);
        assertThat(Cfg05Environment.exercise1(new MockEnvironment().withProperty("cfg05.exercise.retries", "7")))
                .isEqualTo(7);
    }

    @Test
    @DisplayName("Ćwiczenie 2: ExpressShipping istnieje tylko przy cfg05.exercise.express=true")
    void exercise2() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(Cfg05Environment.ExpressShipping.class);
        runner.run(context -> assertThat(context).doesNotHaveBean(Cfg05Environment.ExpressShipping.class));
        runner.withPropertyValues("cfg05.exercise.express=false")
                .run(context -> assertThat(context).doesNotHaveBean(Cfg05Environment.ExpressShipping.class));
        runner.withPropertyValues("cfg05.exercise.express=true")
                .run(context -> assertThat(context).hasSingleBean(Cfg05Environment.ExpressShipping.class));
    }

    @Test
    @DisplayName("Ćwiczenie 3: wartość klucza z podstawionej zmiennej środowiska — działają oba zapisy nazwy")
    void exercise3() {
        // zapis z dokumentacji Boota (myślnik usunięty) — zadziała tylko przy źródle o nazwie "systemEnvironment"
        assertThat(Cfg05Environment.exercise3(Map.of("CFG05_EXERCISE_MAXDAYS", "4"))).isEqualTo(4);
        // starszy zapis (myślnik → podkreślnik)
        assertThat(Cfg05Environment.exercise3(Map.of("CFG05_EXERCISE_MAX_DAYS", "9"))).isEqualTo(9);
        assertThat(Cfg05Environment.exercise3(Map.of("INNA_ZMIENNA", "1"))).isEqualTo(-1);
    }
}
