package s00_start.solutions.start03_first_application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Start03FirstApplicationExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Start03FirstApplicationSolutionsTest {

    static final String MAIN_CLASS = "s00_start.start03_first_application.Start03Application";

    @Test
    @DisplayName("Wzorzec 1: powitanie z obsługą pustego imienia")
    void solution1() {
        assertThat(Start03FirstApplicationSolutions.solution1("  Ola ")).isEqualTo("Cześć, Ola!");
        assertThat(Start03FirstApplicationSolutions.solution1("   ")).isEqualTo("Cześć, nieznajomy!");
        assertThat(Start03FirstApplicationSolutions.solution1("")).isEqualTo("Cześć, nieznajomy!");
        assertThat(Start03FirstApplicationSolutions.solution1(null)).isEqualTo("Cześć, nieznajomy!");
    }

    @Test
    @DisplayName("Wzorzec 2: port z linii logu")
    void solution2() {
        assertThat(Start03FirstApplicationSolutions.solution2("Tomcat started on port 8080 (http) with context path '/'"))
                .isEqualTo(8080);
        assertThat(Start03FirstApplicationSolutions.solution2("Tomcat started on port 8081 (http)")).isEqualTo(8081);
        assertThatThrownBy(() -> Start03FirstApplicationSolutions.solution2("Started Start03Application in 1.2 seconds"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 3: polecenie uruchomienia z linii poleceń")
    void solution3() {
        assertThat(Start03FirstApplicationSolutions.solution3("s00-start", MAIN_CLASS, false))
                .isEqualTo("./mvnw -pl s00-start spring-boot:run -Dspring-boot.run.main-class=" + MAIN_CLASS);
        assertThat(Start03FirstApplicationSolutions.solution3("s00-start", MAIN_CLASS, true))
                .isEqualTo("mvnw.cmd -pl s00-start spring-boot:run -Dspring-boot.run.main-class=" + MAIN_CLASS);
        assertThatThrownBy(() -> Start03FirstApplicationSolutions.solution3("s00-start", "s00_start.Foo", false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
