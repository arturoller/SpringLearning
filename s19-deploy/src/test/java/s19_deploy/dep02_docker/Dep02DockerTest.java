package s19_deploy.dep02_docker;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Dep02Docker. NIE uruchamiają Dockera (działają wszędzie) — czytają Dockerfile i .dockerignore modułu
 * jako zwykłe pliki tekstowe. To zwykłe testy jednostkowe: Spring nie jest tu potrzebny.
 */
class Dep02DockerTest {

    static String dockerfile;
    static String dockerignore;

    @BeforeAll
    static void readFiles() throws IOException {
        // surefire i ▶ w IntelliJ uruchamiają testy w katalogu modułu; zapasowo próbujemy też ścieżki od korzenia repozytorium
        Path dir = Files.exists(Path.of("Dockerfile")) ? Path.of(".") : Path.of("s19-deploy");
        dockerfile = Files.readString(dir.resolve("Dockerfile"), StandardCharsets.UTF_8);
        dockerignore = Files.readString(dir.resolve(".dockerignore"), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("1. Dockerfile jest wielostopniowy, a obrazy bazowe mają jawny tag (nie latest) i końcowy to JRE")
    void multiStageWithPinnedImages() {
        assertThat(DockerfileFacts.stageCount(dockerfile)).isEqualTo(2);

        List<String> images = baseImages(dockerfile);
        assertThat(images).containsExactly("eclipse-temurin:25-jdk", "eclipse-temurin:25-jre");
        assertThat(images).allSatisfy(image -> {
            assertThat(image).contains(":");
            assertThat(image).doesNotEndWith(":latest");
        });
        assertThat(images.getLast()).as("obraz końcowy ma być samym JRE, bez JDK").endsWith("-jre");
    }

    @Test
    @DisplayName("2. Warstwy są kopiowane od najrzadziej do najczęściej zmienianej: dependencies, loader, snapshot, application")
    void layersOrder() {
        List<String> copies = DockerfileFacts.instructions(dockerfile).stream()
                .filter(i -> i.startsWith("COPY --from=extract"))
                .map(i -> i.replaceAll(".*/extracted/([a-z-]+)/.*", "$1"))
                .toList();
        assertThat(copies).containsExactly("dependencies", "spring-boot-loader", "snapshot-dependencies", "application");
    }

    @Test
    @DisplayName("3. Kontener nie działa jako root — ostatnia instrukcja USER to zwykły użytkownik")
    void runsAsNonRoot() {
        assertThat(DockerfileFacts.lastUser(dockerfile)).isEqualTo("app").isNotEqualTo("root");
        // porównanie: Dockerfile bez USER działa jako root
        assertThat(DockerfileFacts.lastUser("FROM eclipse-temurin:25-jre\nENTRYPOINT [\"java\"]")).isEqualTo("root");
    }

    @Test
    @DisplayName("4. Pamięć JVM w kontenerze: ustawione -XX:MaxRAMPercentage (procent limitu, nie sztywne -Xmx)")
    void memoryPercentage() {
        assertThat(dockerfile).contains("-XX:MaxRAMPercentage=75.0");
        assertThat(dockerfile).doesNotContain("-Xmx");
    }

    @Test
    @DisplayName("5. ENTRYPOINT ma formę exec (tablica), więc Java jest procesem nr 1 i dostaje SIGTERM")
    void entrypointIsExecForm() {
        assertThat(DockerfileFacts.hasExecFormEntrypoint(dockerfile)).isTrue();
        assertThat(DockerfileFacts.hasExecFormEntrypoint("ENTRYPOINT java -jar app.jar")).isFalse();
        assertThat(DockerfileFacts.hasExecFormEntrypoint("FROM x:1")).isFalse();
    }

    @Test
    @DisplayName("6. .dockerignore ignoruje wszystko i dopuszcza tylko gotowy JAR")
    void dockerignoreAllowsOnlyJar() {
        List<String> lines = dockerignore.lines().map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#")).toList();
        assertThat(lines).containsExactly("*", "!target/s19-deploy-*.jar");
    }

    @Test
    @DisplayName("Pomocnicze: instrukcje łączą linie kończące się ukośnikiem i pomijają komentarze")
    void instructionsParsing() {
        String text = "# komentarz\nFROM a:1\n\nRUN one \\\n    two\nUSER x\n";
        assertThat(DockerfileFacts.instructions(text)).containsExactly("FROM a:1", "RUN one  two", "USER x");
    }

    /** Obrazy z instrukcji FROM (pierwszy argument) — wyrażeniem regularnym, niezależnie od DockerfileFacts. */
    private static List<String> baseImages(String text) {
        Matcher matcher = Pattern.compile("(?m)^FROM\\s+(\\S+)").matcher(text);
        return matcher.results().map(result -> result.group(1)).toList();
    }
}
