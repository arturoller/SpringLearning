package s03_boot_basics.solutions.boot04_starters;

import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Rozwiązania wzorcowe ćwiczeń lekcji Boot04Starters — zajrzyj dopiero po własnej próbie! */
public final class Boot04StartersSolutions {

    /** Zmiany nazw starterów Boot 3 → Boot 4 (lekcja, sekcja 2). Porównujemy CAŁE nazwy. */
    private static final Map<String, String> RENAMED = Map.of(
            "spring-boot-starter-web", "spring-boot-starter-webmvc",
            "spring-boot-starter-web-services", "spring-boot-starter-webservices",
            "spring-boot-starter-oauth2-client", "spring-boot-starter-security-oauth2-client",
            "spring-boot-starter-oauth2-resource-server", "spring-boot-starter-security-oauth2-resource-server",
            "spring-boot-starter-oauth2-authorization-server", "spring-boot-starter-security-oauth2-authorization-server",
            "spring-boot-starter-aop", "spring-boot-starter-aspectj");

    private Boot04StartersSolutions() {
    }

    /** Rozwiązanie 1: filtr ClassUtils.isPresent, bez powtórzeń, posortowane. */
    public static List<String> solution1(List<String> classNames) {
        return classNames.stream()
                .filter(name -> ClassUtils.isPresent(name, null))
                .distinct()
                .sorted()
                .toList();
    }

    /** Rozwiązanie 2: TreeMap — klucze posortowane (wielkie litery przed małymi, jak w String.compareTo). */
    public static Map<String, String> solution2() {
        Map<String, String> starters = new TreeMap<>();
        starters.put("REST API (Spring MVC)", "spring-boot-starter-webmvc");
        starters.put("JSON", "spring-boot-starter-jackson");
        starters.put("baza danych przez JPA", "spring-boot-starter-data-jpa");
        starters.put("walidacja danych", "spring-boot-starter-validation");
        starters.put("AOP (aspekty)", "spring-boot-starter-aspectj");
        starters.put("migracje bazy (Flyway)", "spring-boot-starter-flyway");
        starters.put("testy kontrolerów MVC", "spring-boot-starter-webmvc-test");
        return starters;
    }

    /** Rozwiązanie 3: tylko nazwy z mapy zmian (get po całej nazwie — „webmvc” nie zostanie pomylony z „web”). */
    public static Map<String, String> solution3(List<String> starters) {
        Map<String, String> changes = new TreeMap<>();
        for (String starter : starters) {
            String replacement = RENAMED.get(starter);
            if (replacement != null) {
                changes.put(starter, replacement);
            }
        }
        return changes;
    }
}
