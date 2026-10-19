package s19_deploy.solutions.dep03_production_config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Rozwiązania wzorcowe ćwiczeń lekcji Dep03ProductionConfig — zajrzyj dopiero po własnej próbie! */
public final class Dep03ProductionConfigSolutions {

    private static final List<String> SECRET_WORDS = List.of("password", "secret", "token", "key");

    private Dep03ProductionConfigSolutions() {
    }

    static String solution1(String propertyKey) {
        return propertyKey.replace('.', '_').replace("-", "").toUpperCase(Locale.ROOT);
    }

    private static boolean looksSecret(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return SECRET_WORDS.stream().anyMatch(lower::contains);
    }

    static Map<String, String> solution2(Map<String, String> properties) {
        Map<String, String> result = new TreeMap<>();
        properties.forEach((key, value) -> result.put(key, looksSecret(key) ? "******" : value));
        return result;
    }

    static List<String> solution3(Map<String, String> properties) {
        List<String> problems = new ArrayList<>();
        if ("true".equals(properties.get("debug"))) {
            problems.add("DEBUG_WLACZONY");
        }
        new TreeMap<>(properties).forEach((key, value) -> {
            boolean placeholder = value.startsWith("${") && value.endsWith("}");
            if (looksSecret(key) && !value.isEmpty() && !placeholder) {
                problems.add("SEKRET_W_PLIKU:" + key);
            }
        });
        if (!properties.getOrDefault("spring.profiles.active", "").contains("prod")) {
            problems.add("PROFIL_PROD_NIEAKTYWNY");
        }
        if ("always".equals(properties.get("management.endpoint.health.show-details"))) {
            problems.add("SZCZEGOLY_ZDROWIA_PUBLICZNE");
        }
        return problems;
    }
}
