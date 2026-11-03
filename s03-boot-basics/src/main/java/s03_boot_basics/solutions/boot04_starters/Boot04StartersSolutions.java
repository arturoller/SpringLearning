package s03_boot_basics.solutions.boot04_starters;

import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Rozwiązania wzorcowe ćwiczeń lekcji Boot04Starters — zajrzyj dopiero po własnej próbie! */
public final class Boot04StartersSolutions {

    private static final Map<String, String> RENAMED_STARTERS = Map.of(
            "spring-boot-starter-web", "spring-boot-starter-webmvc",
            "spring-boot-starter-aop", "spring-boot-starter-aspectj",
            "spring-boot-starter-oauth2-client", "spring-boot-starter-security-oauth2-client",
            "spring-boot-starter-oauth2-resource-server", "spring-boot-starter-security-oauth2-resource-server");

    private Boot04StartersSolutions() {
    }

    /** Rozwiązanie 1: tablica zmian + wartość domyślna = ta sama nazwa (nic się nie zmieniło). */
    public static String solution1Boot4StarterName(String boot3ArtifactId) {
        return RENAMED_STARTERS.getOrDefault(boot3ArtifactId, boot3ArtifactId);
    }

    /** Rozwiązanie 2: ClassUtils.isPresent nie rzuca wyjątku — zwraca false dla brakującej klasy. */
    public static List<String> solution2MissingClasses(ClassLoader classLoader, String... classNames) {
        return Arrays.stream(classNames)
                .filter(name -> !ClassUtils.isPresent(name, classLoader))
                .sorted()
                .toList();
    }

    /** Rozwiązanie 3: kolejność sprawdzeń ma znaczenie — najpierw „oficjalny”, bo przedrostek jest zarezerwowany. */
    public static String solution3ClassifyStarterName(String artifactId) {
        if (artifactId.startsWith("spring-boot-starter")) {
            return "oficjalny";
        }
        if (artifactId.endsWith("-spring-boot-starter") && !artifactId.startsWith("spring-")) {
            return "własny";
        }
        return "niepoprawny";
    }

    /** Rozwiązanie 4: obcinamy ozdobniki drzewa i dopisek (optional); zakres jest ostatnim członem po dwukropkach. */
    public static String solution4ScopeOf(String treeLine) {
        String coordinates = treeLine
                .replaceFirst("^[\\s|+\\\\-]+", "")
                .replace(" (optional)", "")
                .trim();
        String[] parts = coordinates.split(":");
        return parts[parts.length - 1];
    }
}
