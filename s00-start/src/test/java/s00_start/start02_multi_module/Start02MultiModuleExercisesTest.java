package s00_start.start02_multi_module;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Start02MultiModule. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Start02MultiModuleExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: technologia → starter")
    void exercise1() {
        assertThat(Start02MultiModule.exercise1("web")).isEqualTo("spring-boot-starter-webmvc");
        assertThat(Start02MultiModule.exercise1("security")).isEqualTo("spring-boot-starter-security");
        assertThatThrownBy(() -> Start02MultiModule.exercise1("kawa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nieznana technologia: kawa");
    }

    @Test
    @DisplayName("Ćwiczenie 2: starter → zależność testowa")
    void exercise2() {
        assertThat(Start02MultiModule.exercise2("spring-boot-starter-webmvc")).isEqualTo("spring-boot-starter-webmvc-test");
        assertThatThrownBy(() -> Start02MultiModule.exercise2("lombok")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 3: klasy, których brakuje na classpath, posortowane")
    void exercise3() {
        List<String> classes = List.of(
                "org.springframework.security.core.Authentication",
                "org.springframework.web.servlet.DispatcherServlet",
                "org.springframework.data.jpa.repository.JpaRepository");
        assertThat(Start02MultiModule.exercise3(classes)).containsExactly(
                "org.springframework.data.jpa.repository.JpaRepository",
                "org.springframework.security.core.Authentication");
    }
}
