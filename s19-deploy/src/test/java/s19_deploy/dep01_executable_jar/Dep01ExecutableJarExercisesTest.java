package s19_deploy.dep01_executable_jar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Dep01ExecutableJar. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Dep01ExecutableJarExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: polecenie java -jar z opcjami JVM przed -jar i argumentami po nazwie JAR-a")
    void exercise1() {
        assertThat(Dep01ExecutableJar.exercise1("app.jar", Map.of("file.encoding", "UTF-8"),
                Map.of("shop.name", "Test", "server.port", "8099")))
                .isEqualTo("java -Dfile.encoding=UTF-8 -jar app.jar --server.port=8099 --shop.name=Test");
        assertThat(Dep01ExecutableJar.exercise1("app.jar", Map.of(), Map.of())).isEqualTo("java -jar app.jar");
        assertThat(Dep01ExecutableJar.exercise1("app.jar", Map.of("a", "1", "b", "2"), Map.of()))
                .isEqualTo("java -Da=1 -Db=2 -jar app.jar");
    }

    @Test
    @DisplayName("Ćwiczenie 2: liczenie bibliotek w BOOT-INF/lib")
    void exercise2() {
        List<String> entries = List.of("META-INF/MANIFEST.MF", "BOOT-INF/lib/", "BOOT-INF/lib/a-1.0.jar",
                "BOOT-INF/lib/b-2.0.jar", "BOOT-INF/classes/A.class", "org/springframework/boot/loader/launch/JarLauncher.class");
        assertThat(Dep01ExecutableJar.exercise2(entries)).isEqualTo(2);
        assertThat(Dep01ExecutableJar.exercise2(List.of())).isZero();
    }

    @Test
    @DisplayName("Ćwiczenie 3: przydział wpisu JAR-a do warstwy")
    void exercise3() {
        assertThat(Dep01ExecutableJar.exercise3("BOOT-INF/lib/logback-core-1.5.38.jar")).isEqualTo("dependencies");
        assertThat(Dep01ExecutableJar.exercise3("BOOT-INF/lib/modul-1.0-SNAPSHOT.jar")).isEqualTo("snapshot-dependencies");
        assertThat(Dep01ExecutableJar.exercise3("org/springframework/boot/loader/launch/JarLauncher.class"))
                .isEqualTo("spring-boot-loader");
        assertThat(Dep01ExecutableJar.exercise3("BOOT-INF/classes/A.class")).isEqualTo("application");
        assertThat(Dep01ExecutableJar.exercise3("META-INF/MANIFEST.MF")).isEqualTo("application");
    }
}
