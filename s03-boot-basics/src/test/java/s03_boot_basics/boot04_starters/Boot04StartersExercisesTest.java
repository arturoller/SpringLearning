package s03_boot_basics.boot04_starters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.yaml.snakeyaml.Yaml;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Boot04Starters. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Boot04StartersExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: klasy obecne na classpath, posortowane")
    void exercise1() {
        assertThat(Boot04Starters.exercise1(List.of(
                "org.yaml.snakeyaml.Yaml",
                "org.apache.catalina.startup.Tomcat",
                "ch.qos.logback.classic.Logger",
                "tools.jackson.databind.ObjectMapper")))
                .containsExactly("ch.qos.logback.classic.Logger", "org.yaml.snakeyaml.Yaml");
    }

    @Test
    @DisplayName("Ćwiczenie 2: nazwa jara, z którego pochodzi klasa")
    void exercise2() {
        assertThat(Boot04Starters.exercise2(ApplicationContext.class)).isEqualTo("spring-context-7.0.9.jar");
        assertThat(Boot04Starters.exercise2(Yaml.class)).isEqualTo("snakeyaml-2.6.jar");
    }

    @Test
    @DisplayName("Ćwiczenie 3: zależności o danym zasięgu z dependency:tree")
    void exercise3() {
        assertThat(Boot04Starters.exercise3(Boot04Starters.DEPENDENCY_TREE_SAMPLE, "test")).containsExactly(
                "assertj-core:3.27.7", "junit-jupiter:6.0.3", "mockito-core:5.23.0", "spring-boot-starter-test:4.1.1");
        assertThat(Boot04Starters.exercise3(Boot04Starters.DEPENDENCY_TREE_SAMPLE, "compile")).containsExactly(
                "jakarta.annotation-api:3.0.0", "logback-classic:1.5.38", "logback-core:1.5.38", "snakeyaml:2.6",
                "spring-boot-starter-logging:4.1.1", "spring-boot-starter:4.1.1");
    }
}
