package s19_deploy.dep01_executable_jar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Dep01ExecutableJar. Żaden nie wymaga zbudowanego JAR-a (faza test jest PRZED package) —
 * sprawdzają konfigurację budowy i uruchamiają aplikację tak, jak robi to JarLauncher.
 */
class Dep01ExecutableJarTest {

    /** Wpisy z prawdziwego wyniku {@code jar tf s19-deploy-1.0-SNAPSHOT.jar} (skrócone). */
    private static final List<String> REAL_JAR_ENTRIES = List.of(
            "META-INF/MANIFEST.MF",
            "org/springframework/boot/loader/launch/JarLauncher.class",
            "BOOT-INF/classes/s19_deploy/dep01_executable_jar/Dep01Application.class",
            "BOOT-INF/lib/logback-classic-1.5.38.jar",
            "BOOT-INF/lib/spring-webmvc-7.0.9.jar",
            "BOOT-INF/classpath.idx",
            "BOOT-INF/layers.idx");

    @Test
    @DisplayName("1. pom.xml ma spring-boot-maven-plugin, a mainClass wskazuje klasę z metodą main i @SpringBootApplication")
    void pomHasPluginAndMainClass() throws Exception {
        // surefire uruchamia testy w katalogu modułu; ▶ w IntelliJ też (domyślnie $MODULE_WORKING_DIR$)
        Path pom = Files.exists(Path.of("pom.xml")) ? Path.of("pom.xml") : Path.of("s19-deploy/pom.xml");
        String xml = Files.readString(pom, StandardCharsets.UTF_8);

        assertThat(xml).contains("<artifactId>spring-boot-maven-plugin</artifactId>");
        Matcher matcher = Pattern.compile("<mainClass>([^<]+)</mainClass>").matcher(xml);
        assertThat(matcher.find()).as("w pom.xml brakuje <mainClass>").isTrue();

        Class<?> mainClass = Class.forName(matcher.group(1).trim());
        Method main = mainClass.getMethod("main", String[].class);   // klasa musi mieć public static void main(String[])
        assertThat(Modifier.isStatic(main.getModifiers())).isTrue();
        assertThat(mainClass.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(mainClass).isEqualTo(Dep01Application.class);
    }

    @Test
    @DisplayName("2. Aplikacja czyta ustawienia z domyślnych wartości; w teście nie ma dostępu do zbudowanego JAR-a")
    void defaultsWithoutArguments() {
        try (ConfigurableApplicationContext context = SpringApplication.run(Dep01Application.class, "--server.port=0")) {
            ShopInfoController controller = context.getBean(ShopInfoController.class);
            assertThat(controller.shop()).containsEntry("name", "Sklep domyślny").containsEntry("greeting", "Witamy");
        }
    }

    @Test
    @DisplayName("3. Argumenty z linii poleceń (--klucz=wartość) wygrywają z wartością domyślną — tak jak po java -jar")
    void commandLineArgumentsWin() {
        // Dokładnie to robi main(String[] args) → SpringApplication.run(..., args). --server.port=0 = losowy wolny port.
        try (ConfigurableApplicationContext context = SpringApplication.run(Dep01Application.class,
                "--server.port=0", "--shop.name=Z argumentu", "--shop.greeting=Cześć")) {
            Map<String, String> shop = context.getBean(ShopInfoController.class).shop();
            assertThat(shop).containsEntry("name", "Z argumentu").containsEntry("greeting", "Cześć");
        }
    }

    @Test
    @DisplayName("3. PUŁAPKA: argument w postaci -Dklucz=wartość PO nazwie JAR-a nie jest ustawieniem Springa")
    void dashDArgumentIsNotAProperty() {
        try (ConfigurableApplicationContext context = SpringApplication.run(Dep01Application.class,
                "--server.port=0", "-Dshop.name=Zle")) {
            assertThat(context.getBean(ShopInfoController.class).shop()).containsEntry("name", "Sklep domyślny");
        }
    }

    @Test
    @DisplayName("4. Przydział wpisów JAR-a do warstw — na prawdziwych ścieżkach z jar tf")
    void layersOfRealEntries() {
        assertThat(JarLayers.layerOf("BOOT-INF/lib/logback-classic-1.5.38.jar")).isEqualTo("dependencies");
        assertThat(JarLayers.layerOf("BOOT-INF/lib/spring-webmvc-7.0.9.jar")).isEqualTo("dependencies");
        assertThat(JarLayers.layerOf("org/springframework/boot/loader/launch/JarLauncher.class")).isEqualTo("spring-boot-loader");
        assertThat(JarLayers.layerOf("BOOT-INF/lib/s19-modul-1.0-SNAPSHOT.jar")).isEqualTo("snapshot-dependencies");
        assertThat(JarLayers.layerOf("BOOT-INF/classes/s19_deploy/dep01_executable_jar/Dep01Application.class"))
                .isEqualTo("application");
        assertThat(JarLayers.layerOf("META-INF/MANIFEST.MF")).isEqualTo("application");
        assertThat(JarLayers.layerOf("BOOT-INF/layers.idx")).isEqualTo("application");

        // z prawdziwych wpisów: dwie biblioteki w warstwie dependencies, reszta to loader i aplikacja
        assertThat(REAL_JAR_ENTRIES.stream().filter(e -> JarLayers.layerOf(e).equals("dependencies")).toList())
                .hasSize(2);
    }
}
