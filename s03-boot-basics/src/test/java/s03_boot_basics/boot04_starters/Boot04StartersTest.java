package s03_boot_basics.boot04_starters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.SpringVersion;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Boot04Starters. Sprawdzamy to, co da się sprawdzić w działającej aplikacji: co jest na ścieżce
 * klas (classpath), skąd pochodzi kod, jakie wersje przyszły z BOM-u i jakie beany utworzył Boot.
 * {@code @SpringBootTest} znajduje Boot04Application w tym samym pakiecie.
 */
@SpringBootTest
class Boot04StartersTest {

    @Autowired
    ConfigurableApplicationContext context;

    /** Jar, z którego pochodzi plik MANIFEST.MF zawierający w adresie podany fragment nazwy. */
    private static List<String> entriesOfJar(String jarNameFragment) throws IOException {
        URL manifest = Collections.list(Boot04StartersTest.class.getClassLoader().getResources("META-INF/MANIFEST.MF"))
                .stream()
                .filter(url -> url.toString().contains(jarNameFragment))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Nie ma na ścieżce klas jaru: " + jarNameFragment));
        JarURLConnection connection = (JarURLConnection) manifest.openConnection();
        connection.setUseCaches(false);   // własna kopia JarFile — możemy ją bezpiecznie zamknąć
        try (JarFile jar = connection.getJarFile()) {
            return jar.stream().map(JarEntry::getName).toList();
        }
    }

    @Test
    @DisplayName("1. Starter to artefakt bez kodu; autokonfiguracja Jacksona leży w module spring-boot-jackson")
    void starterHasNoCode() throws IOException {
        List<String> starter = entriesOfJar("spring-boot-starter-jackson-");
        assertThat(starter).noneMatch(name -> name.endsWith(".class"));
        assertThat(starter).contains("META-INF/MANIFEST.MF");

        List<String> module = entriesOfJar("/spring-boot-jackson-");
        assertThat(module).contains(
                "org/springframework/boot/jackson/autoconfigure/JacksonAutoConfiguration.class",
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");

        String location = JacksonAutoConfiguration.class.getProtectionDomain().getCodeSource().getLocation().toString();
        assertThat(location).contains("spring-boot-jackson-");
    }

    @Test
    @DisplayName("3. BOM: wersje bibliotek przyszły z spring-boot-dependencies — Boot 4.1.1, Spring Framework 7, Jackson 3")
    void versionsComeFromTheBom() {
        // Przy aktualizacji Boota w parent pom.xml zmień tę wartość (CLAUDE.md, sekcja 3).
        assertThat(SpringBootVersion.getVersion()).isEqualTo("4.1.1");
        assertThat(SpringVersion.getVersion()).startsWith("7.");
        assertThat(JsonMapper.class.getPackage().getImplementationVersion()).startsWith("3.");
    }

    @Test
    @DisplayName("4. Startery decydują o ścieżce klas, a ścieżka klas — o beanach: jest JSON, nie ma weba")
    void classpathDecidesWhatBootConfigures() {
        assertThat(ClassUtils.isPresent("tools.jackson.databind.json.JsonMapper", null)).isTrue();
        assertThat(ClassUtils.isPresent("org.springframework.web.servlet.DispatcherServlet", null)).isFalse();
        assertThat(TechnologyCheck.presentTechnologies())
                .containsExactly("JSON (Jackson 3)", "Logowanie (Logback)", "YAML");

        assertThat(context.getBeanNamesForType(JsonMapper.class)).containsExactly("jacksonJsonMapper");
        assertThat(Arrays.stream(context.getBeanDefinitionNames()).map(String::toLowerCase))
                .noneMatch(name -> name.contains("dispatcherservlet") || name.contains("tomcat")
                        || name.contains("webserver"));
    }
}
