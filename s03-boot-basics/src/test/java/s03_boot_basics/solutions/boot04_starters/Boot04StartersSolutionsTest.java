package s03_boot_basics.solutions.boot04_starters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/** Sprawdzenia z Boot04StartersExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Boot04StartersSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: klasy obecne na ścieżce klas modułu — posortowane, bez powtórzeń")
    void solution1() {
        List<String> result = Boot04StartersSolutions.solution1(List.of(
                "tools.jackson.databind.json.JsonMapper",
                "org.springframework.web.servlet.DispatcherServlet",
                "org.yaml.snakeyaml.Yaml",
                "jakarta.persistence.EntityManager",
                "ch.qos.logback.classic.Logger",
                "tools.jackson.databind.json.JsonMapper"));
        assertThat(result).containsExactly(
                "ch.qos.logback.classic.Logger",
                "org.yaml.snakeyaml.Yaml",
                "tools.jackson.databind.json.JsonMapper");
    }

    @Test
    @DisplayName("Wzorzec 2: potrzeba → starter Boota 4 w TreeMap")
    void solution2() {
        Map<String, String> result = Boot04StartersSolutions.solution2();
        assertThat(result).isInstanceOf(TreeMap.class);
        assertThat(result).containsExactly(
                entry("AOP (aspekty)", "spring-boot-starter-aspectj"),
                entry("JSON", "spring-boot-starter-jackson"),
                entry("REST API (Spring MVC)", "spring-boot-starter-webmvc"),
                entry("baza danych przez JPA", "spring-boot-starter-data-jpa"),
                entry("migracje bazy (Flyway)", "spring-boot-starter-flyway"),
                entry("testy kontrolerów MVC", "spring-boot-starter-webmvc-test"),
                entry("walidacja danych", "spring-boot-starter-validation"));
    }

    @Test
    @DisplayName("Wzorzec 3: przestarzałe i usunięte nazwy starterów → nazwy z Boota 4")
    void solution3() {
        Map<String, String> result = Boot04StartersSolutions.solution3(List.of(
                "spring-boot-starter-web",
                "spring-boot-starter-jackson",
                "spring-boot-starter-aop",
                "spring-boot-starter-oauth2-client",
                "spring-boot-starter-webmvc",
                "spring-boot-starter-web-services"));
        assertThat(result).isInstanceOf(TreeMap.class);
        assertThat(result).containsExactly(
                entry("spring-boot-starter-aop", "spring-boot-starter-aspectj"),
                entry("spring-boot-starter-oauth2-client", "spring-boot-starter-security-oauth2-client"),
                entry("spring-boot-starter-web", "spring-boot-starter-webmvc"),
                entry("spring-boot-starter-web-services", "spring-boot-starter-webservices"));
        assertThat(Boot04StartersSolutions.solution3(List.of("spring-boot-starter-webmvc", "spring-boot-starter-jackson")))
                .isEmpty();
    }
}
