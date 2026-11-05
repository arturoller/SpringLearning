package s03_boot_basics.boot04_starters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Boot04Starters. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Boot04StartersExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: nazwy starterów z Boot 3 zamienione na nazwy z Boot 4")
    void exercise1() {
        assertThat(Boot04Starters.exercise1Boot4StarterName("spring-boot-starter-web")).isEqualTo("spring-boot-starter-webmvc");
        assertThat(Boot04Starters.exercise1Boot4StarterName("spring-boot-starter-aop")).isEqualTo("spring-boot-starter-aspectj");
        assertThat(Boot04Starters.exercise1Boot4StarterName("spring-boot-starter-oauth2-client"))
                .isEqualTo("spring-boot-starter-security-oauth2-client");
        assertThat(Boot04Starters.exercise1Boot4StarterName("spring-boot-starter-oauth2-resource-server"))
                .isEqualTo("spring-boot-starter-security-oauth2-resource-server");
        assertThat(Boot04Starters.exercise1Boot4StarterName("spring-boot-starter-data-jpa")).isEqualTo("spring-boot-starter-data-jpa");
    }

    @Test
    @DisplayName("Ćwiczenie 2: lista klas, których brakuje na classpath")
    void exercise2() {
        ClassLoader loader = getClass().getClassLoader();
        assertThat(Boot04Starters.exercise2MissingClasses(loader,
                "org.h2.Driver", "org.apache.catalina.startup.Tomcat", "jakarta.servlet.Servlet", "java.lang.String"))
                .containsExactly("jakarta.servlet.Servlet", "org.apache.catalina.startup.Tomcat");
        assertThat(Boot04Starters.exercise2MissingClasses(loader, "org.h2.Driver")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: klasyfikacja nazw starterów")
    void exercise3() {
        assertThat(Boot04Starters.exercise3ClassifyStarterName("spring-boot-starter-jdbc")).isEqualTo("oficjalny");
        assertThat(Boot04Starters.exercise3ClassifyStarterName("spring-boot-starter-acme")).isEqualTo("oficjalny");
        assertThat(Boot04Starters.exercise3ClassifyStarterName("acme-spring-boot-starter")).isEqualTo("własny");
        assertThat(Boot04Starters.exercise3ClassifyStarterName("acme-starter")).isEqualTo("niepoprawny");
        assertThat(Boot04Starters.exercise3ClassifyStarterName("spring-acme-spring-boot-starter")).isEqualTo("niepoprawny");
    }

    @Test
    @DisplayName("Ćwiczenie 4: zakres zależności odczytany z linii dependency:tree")
    void exercise4() {
        assertThat(Boot04Starters.exercise4ScopeOf("|  +- org.springframework.boot:spring-boot-jdbc:jar:4.1.1:compile")).isEqualTo("compile");
        assertThat(Boot04Starters.exercise4ScopeOf("+- com.h2database:h2:jar:2.4.240:runtime")).isEqualTo("runtime");
        assertThat(Boot04Starters.exercise4ScopeOf("\\- org.springframework.boot:spring-boot-devtools:jar:4.1.1:compile (optional)")).isEqualTo("compile");
        assertThat(Boot04Starters.exercise4ScopeOf("\\- org.springframework.boot:spring-boot-starter-test:jar:4.1.1:test")).isEqualTo("test");
    }
}
