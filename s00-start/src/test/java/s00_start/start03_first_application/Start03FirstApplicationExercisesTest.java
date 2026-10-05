package s00_start.start03_first_application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Start03FirstApplication. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Start03FirstApplicationExercisesTest {

    static final String MAIN_CLASS = "s00_start.start03_first_application.Start03Application";

    @Test
    @DisplayName("Ćwiczenie 1: powitanie z obsługą pustego imienia")
    void exercise1() {
        assertThat(Start03FirstApplication.exercise1("  Ola ")).isEqualTo("Cześć, Ola!");
        assertThat(Start03FirstApplication.exercise1("   ")).isEqualTo("Cześć, nieznajomy!");
        assertThat(Start03FirstApplication.exercise1("")).isEqualTo("Cześć, nieznajomy!");
        assertThat(Start03FirstApplication.exercise1(null)).isEqualTo("Cześć, nieznajomy!");
    }

    @Test
    @DisplayName("Ćwiczenie 2: port z linii logu")
    void exercise2() {
        assertThat(Start03FirstApplication.exercise2("Tomcat started on port 8080 (http) with context path '/'"))
                .isEqualTo(8080);
        assertThat(Start03FirstApplication.exercise2("Tomcat started on port 8081 (http)")).isEqualTo(8081);
        assertThatThrownBy(() -> Start03FirstApplication.exercise2("Started Start03Application in 1.2 seconds"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 3: polecenie uruchomienia z linii poleceń")
    void exercise3() {
        assertThat(Start03FirstApplication.exercise3("s00-start", MAIN_CLASS, false))
                .isEqualTo("./mvnw -pl s00-start spring-boot:run -Dspring-boot.run.main-class=" + MAIN_CLASS);
        assertThat(Start03FirstApplication.exercise3("s00-start", MAIN_CLASS, true))
                .isEqualTo("mvnw.cmd -pl s00-start spring-boot:run -Dspring-boot.run.main-class=" + MAIN_CLASS);
        assertThatThrownBy(() -> Start03FirstApplication.exercise3("s00-start", "s00_start.Foo", false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
