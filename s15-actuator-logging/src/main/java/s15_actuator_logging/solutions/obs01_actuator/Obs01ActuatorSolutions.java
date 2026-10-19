package s15_actuator_logging.solutions.obs01_actuator;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Rozwiązania wzorcowe ćwiczeń lekcji Obs01Actuator — zajrzyj dopiero po własnej próbie! */
public final class Obs01ActuatorSolutions {

    private static final Set<String> RISKY = Set.of(
            "beans", "configprops", "env", "heapdump", "loggers", "mappings", "shutdown", "threaddump");

    private Obs01ActuatorSolutions() {
    }

    static String solution1(List<String> endpointIds) {
        if (endpointIds.isEmpty()) {
            throw new IllegalArgumentException("Lista punktów końcowych nie może być pusta");
        }
        return "management.endpoints.web.exposure.include=" + String.join(",", endpointIds);
    }

    static boolean solution2(String include, String exclude, String endpointId) {
        String id = endpointId.strip().toLowerCase();
        Set<String> excluded = names(exclude);
        if (excluded.contains("*") || excluded.contains(id)) {
            return false;   // exclude ma pierwszeństwo
        }
        Set<String> included = names(include);
        return included.contains("*") || included.contains(id);
    }

    static List<String> solution3(Set<String> exposedEndpointIds) {
        return exposedEndpointIds.stream().filter(RISKY::contains).sorted().toList();
    }

    private static Set<String> names(String commaSeparated) {
        return Arrays.stream(commaSeparated.split(","))
                .map(String::strip)
                .map(String::toLowerCase)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
    }
}
