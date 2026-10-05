package s00_start.solutions.start02_multi_module;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Start02MultiModuleExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Start02MultiModuleSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: technologia → starter")
    void solution1() {
        assertThat(Start02MultiModuleSolutions.solution1("web")).isEqualTo("spring-boot-starter-webmvc");
        assertThat(Start02MultiModuleSolutions.solution1("security")).isEqualTo("spring-boot-starter-security");
        assertThatThrownBy(() -> Start02MultiModuleSolutions.solution1("kawa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nieznana technologia: kawa");
    }

    @Test
    @DisplayName("Wzorzec 2: starter → zależność testowa")
    void solution2() {
        assertThat(Start02MultiModuleSolutions.solution2("spring-boot-starter-webmvc"))
                .isEqualTo("spring-boot-starter-webmvc-test");
        assertThatThrownBy(() -> Start02MultiModuleSolutions.solution2("lombok"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 3: klasy, których brakuje na classpath, posortowane")
    void solution3() {
        List<String> classes = List.of(
                "org.springframework.security.core.Authentication",
                "org.springframework.web.servlet.DispatcherServlet",
                "org.springframework.data.jpa.repository.JpaRepository");
        assertThat(Start02MultiModuleSolutions.solution3(classes)).containsExactly(
                "org.springframework.data.jpa.repository.JpaRepository",
                "org.springframework.security.core.Authentication");
    }
}
