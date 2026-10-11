package s03_boot_basics.boot04_starters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.core.SpringVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static s03_boot_basics.boot04_starters.ClasspathInspector.isPresent;

/**
 * Testy lekcji Boot04Starters. Zwykłe testy jednostkowe — bez kontekstu Springa: sprawdzamy classpath i jary, a do tego
 * kontener nie jest potrzebny.
 */
class Boot04StartersTest {

    @Test
    @DisplayName("1. Starter spring-boot-starter-4.1.1.jar nie ma ani jednej klasy — tylko pom.xml i META-INF")
    void starterHasNoClasses() {
        assertThat(ClasspathInspector.countClassesInJar("spring-boot-starter-4.1.1.jar")).isZero();
        // dla porównania: jar z autokonfiguracją ma setki klas
        assertThat(ClasspathInspector.countClassesInJar("spring-boot-autoconfigure-4.1.1.jar")).isGreaterThan(100);
    }

    @Test
    @DisplayName("2. spring-boot-starter przynosi Spring, Boota, Logback, SLF4J, SnakeYAML, jakarta.annotation i Micrometer (pytanie 3)")
    void whatStarterBrings() {
        assertThat(isPresent("org.springframework.context.ApplicationContext")).isTrue();
        assertThat(isPresent("org.springframework.boot.autoconfigure.SpringBootApplication")).isTrue();
        assertThat(isPresent("ch.qos.logback.classic.Logger")).isTrue();
        assertThat(isPresent("org.slf4j.Logger")).isTrue();
        assertThat(isPresent("org.yaml.snakeyaml.Yaml")).isTrue();
        assertThat(isPresent("jakarta.annotation.PostConstruct")).isTrue();
        assertThat(isPresent("io.micrometer.observation.Observation")).as("przechodnio ze spring-context").isTrue();
    }

    @Test
    @DisplayName("3. Brak Tomcata, Spring MVC, Jacksona 2 i 3, JDBC; AssertJ widać tylko w testach (pytania 2 i 7)")
    void whatStarterDoesNotBring() {
        assertThat(isPresent("org.apache.catalina.startup.Tomcat")).isFalse();
        assertThat(isPresent("org.springframework.web.servlet.DispatcherServlet")).isFalse();
        assertThat(isPresent("tools.jackson.databind.ObjectMapper")).isFalse();
        assertThat(isPresent("com.fasterxml.jackson.databind.ObjectMapper")).isFalse();
        assertThat(isPresent("org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")).isFalse();
        assertThat(isPresent("org.springframework.jdbc.core.JdbcTemplate")).isFalse();
        // classpath TESTÓW zawiera zależności o zasięgu test:
        assertThat(isPresent("org.assertj.core.api.Assertions")).isTrue();
    }

    @Test
    @DisplayName("4. Boot 4: autokonfiguracja webowa jest w osobnym module spring-boot-webmvc — tu go nie ma")
    void boot4Modularization() {
        assertThat(isPresent("org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration")).isTrue();
        assertThat(isPresent("org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration")).isFalse();
        assertThat(ClasspathInspector.countClassesInJar("spring-boot-webmvc-4.1.1.jar")).as("brak jara").isEqualTo(-1);
    }

    @Test
    @DisplayName("5. Wersje z BOM-u: Spring Boot 4.1.1 i Spring Framework 7.0.9")
    void versionsFromBom() {
        assertThat(SpringBootVersion.getVersion()).isEqualTo("4.1.1");
        assertThat(SpringVersion.getVersion()).isEqualTo("7.0.9");
    }

    @Test
    @DisplayName("7. getCodeSource().getLocation() mówi, z którego jara pochodzi klasa")
    void whichJar() {
        String location = ApplicationContext.class.getProtectionDomain().getCodeSource().getLocation().toString();
        assertThat(location).endsWith("spring-context-7.0.9.jar");
    }
}
