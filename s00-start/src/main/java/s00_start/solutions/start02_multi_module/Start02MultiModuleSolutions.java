package s00_start.solutions.start02_multi_module;

import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.Map;

/** Rozwiązania wzorcowe ćwiczeń lekcji Start02MultiModule — zajrzyj dopiero po własnej próbie! */
public final class Start02MultiModuleSolutions {

    private static final Map<String, String> STARTERS = Map.of(
            "web", "spring-boot-starter-webmvc",
            "validation", "spring-boot-starter-validation",
            "jpa", "spring-boot-starter-data-jpa",
            "security", "spring-boot-starter-security",
            "actuator", "spring-boot-starter-actuator");

    private Start02MultiModuleSolutions() {
    }

    static String solution1(String technology) {
        String starter = STARTERS.get(technology);
        if (starter == null) {
            throw new IllegalArgumentException("Nieznana technologia: " + technology);
        }
        return starter;
    }

    static String solution2(String starterArtifact) {
        if (!starterArtifact.startsWith("spring-boot-starter-")) {
            throw new IllegalArgumentException("To nie jest starter Spring Boota: " + starterArtifact);
        }
        return starterArtifact + "-test";
    }

    static List<String> solution3(List<String> classNames) {
        ClassLoader loader = Start02MultiModuleSolutions.class.getClassLoader();
        return classNames.stream()
                .filter(name -> !ClassUtils.isPresent(name, loader))
                .sorted()
                .toList();
    }
}
