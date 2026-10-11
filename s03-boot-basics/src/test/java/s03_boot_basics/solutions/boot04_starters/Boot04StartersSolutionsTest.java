package s03_boot_basics.solutions.boot04_starters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.yaml.snakeyaml.Yaml;
import s03_boot_basics.boot04_starters.Boot04Starters;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Boot04StartersExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Boot04StartersSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: klasy obecne na classpath, posortowane")
    void solution1() {
        assertThat(Boot04StartersSolutions.solution1(List.of(
                "org.yaml.snakeyaml.Yaml",
                "org.apache.catalina.startup.Tomcat",
                "ch.qos.logback.classic.Logger",
                "tools.jackson.databind.ObjectMapper")))
                .containsExactly("ch.qos.logback.classic.Logger", "org.yaml.snakeyaml.Yaml");
    }

    @Test
    @DisplayName("Wzorzec 2: nazwa jara, z którego pochodzi klasa")
    void solution2() {
        assertThat(Boot04StartersSolutions.solution2(ApplicationContext.class)).isEqualTo("spring-context-7.0.9.jar");
        assertThat(Boot04StartersSolutions.solution2(Yaml.class)).isEqualTo("snakeyaml-2.6.jar");
    }

    @Test
    @DisplayName("Wzorzec 3: zależności o danym zasięgu z dependency:tree")
    void solution3() {
        assertThat(Boot04StartersSolutions.solution3(Boot04Starters.DEPENDENCY_TREE_SAMPLE, "test")).containsExactly(
                "assertj-core:3.27.7", "junit-jupiter:6.0.3", "mockito-core:5.23.0", "spring-boot-starter-test:4.1.1");
        assertThat(Boot04StartersSolutions.solution3(Boot04Starters.DEPENDENCY_TREE_SAMPLE, "compile")).containsExactly(
                "jakarta.annotation-api:3.0.0", "logback-classic:1.5.38", "logback-core:1.5.38", "snakeyaml:2.6",
                "spring-boot-starter-logging:4.1.1", "spring-boot-starter:4.1.1");
    }
}
