package s19_deploy.solutions.dep01_executable_jar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Dep01ExecutableJarExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Dep01ExecutableJarSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: polecenie java -jar")
    void solution1() {
        assertThat(Dep01ExecutableJarSolutions.solution1("app.jar", Map.of("file.encoding", "UTF-8"),
                Map.of("shop.name", "Test", "server.port", "8099")))
                .isEqualTo("java -Dfile.encoding=UTF-8 -jar app.jar --server.port=8099 --shop.name=Test");
        assertThat(Dep01ExecutableJarSolutions.solution1("app.jar", Map.of(), Map.of())).isEqualTo("java -jar app.jar");
        assertThat(Dep01ExecutableJarSolutions.solution1("app.jar", Map.of("a", "1", "b", "2"), Map.of()))
                .isEqualTo("java -Da=1 -Db=2 -jar app.jar");
    }

    @Test
    @DisplayName("Wzorzec 2: liczenie bibliotek")
    void solution2() {
        List<String> entries = List.of("META-INF/MANIFEST.MF", "BOOT-INF/lib/", "BOOT-INF/lib/a-1.0.jar",
                "BOOT-INF/lib/b-2.0.jar", "BOOT-INF/classes/A.class", "org/springframework/boot/loader/launch/JarLauncher.class");
        assertThat(Dep01ExecutableJarSolutions.solution2(entries)).isEqualTo(2);
        assertThat(Dep01ExecutableJarSolutions.solution2(List.of())).isZero();
    }

    @Test
    @DisplayName("Wzorzec 3: przydział do warstw")
    void solution3() {
        assertThat(Dep01ExecutableJarSolutions.solution3("BOOT-INF/lib/logback-core-1.5.38.jar")).isEqualTo("dependencies");
        assertThat(Dep01ExecutableJarSolutions.solution3("BOOT-INF/lib/modul-1.0-SNAPSHOT.jar")).isEqualTo("snapshot-dependencies");
        assertThat(Dep01ExecutableJarSolutions.solution3("org/springframework/boot/loader/launch/JarLauncher.class"))
                .isEqualTo("spring-boot-loader");
        assertThat(Dep01ExecutableJarSolutions.solution3("BOOT-INF/classes/A.class")).isEqualTo("application");
        assertThat(Dep01ExecutableJarSolutions.solution3("META-INF/MANIFEST.MF")).isEqualTo("application");
    }
}
