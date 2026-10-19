package s19_deploy.dep02_docker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Dep02Docker. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Dep02DockerExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: obraz przypięty do jawnego tagu")
    void exercise1() {
        assertThat(Dep02Docker.exercise1("eclipse-temurin:25-jre")).isTrue();
        assertThat(Dep02Docker.exercise1("eclipse-temurin")).isFalse();
        assertThat(Dep02Docker.exercise1("eclipse-temurin:latest")).isFalse();
        assertThat(Dep02Docker.exercise1("localhost:5000/app")).isFalse();
        assertThat(Dep02Docker.exercise1("localhost:5000/app:1.2")).isTrue();
    }

    @Test
    @DisplayName("Ćwiczenie 2: obrazy bazowe z instrukcji FROM")
    void exercise2() {
        String dockerfile = """
                # komentarz: FROM nic:1
                FROM --platform=linux/amd64 eclipse-temurin:25-jdk AS build
                RUN echo hello
                from eclipse-temurin:25-jre
                COPY --from=build /a /b
                """;
        assertThat(Dep02Docker.exercise2(dockerfile)).containsExactly("eclipse-temurin:25-jdk", "eclipse-temurin:25-jre");
        assertThat(Dep02Docker.exercise2("RUN x")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: linter Dockerfile")
    void exercise3() {
        String good = """
                FROM eclipse-temurin:25-jdk AS extract
                FROM eclipse-temurin:25-jre
                USER app
                ENTRYPOINT ["java", "-jar", "app.jar"]
                """;
        assertThat(Dep02Docker.exercise3(good)).isEmpty();

        String bad = """
                FROM eclipse-temurin AS build
                FROM build
                ENTRYPOINT java -jar app.jar
                """;
        assertThat(Dep02Docker.exercise3(bad)).containsExactly(
                "NIEPRZYPIETY_OBRAZ:eclipse-temurin", "BRAK_USER_NIE_ROOT", "ENTRYPOINT_W_FORMIE_SHELL");

        String rootAgain = "FROM a:1\nUSER app\nUSER root\nENTRYPOINT [\"x\"]\n";
        assertThat(Dep02Docker.exercise3(rootAgain)).containsExactly("BRAK_USER_NIE_ROOT");
        assertThat(Dep02Docker.exercise3("FROM a:1\nUSER 0\n")).containsExactly("BRAK_USER_NIE_ROOT");
    }
}
