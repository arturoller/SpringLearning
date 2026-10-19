package s15_actuator_logging.solutions.obs03_metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Rozwiązania wzorcowe ćwiczeń lekcji Obs03Metrics — zajrzyj dopiero po własnej próbie! */
public final class Obs03MetricsSolutions {

    private Obs03MetricsSolutions() {
    }

    static String solution1(String area, String event) {
        if (area.isBlank() || event.isBlank()) {
            throw new IllegalArgumentException("Obszar i zdarzenie nie mogą być puste");
        }
        return area.strip().toLowerCase() + "." + event.strip().toLowerCase();
    }

    /** Model serii: klucz = nazwa + posortowane tagi (TreeMap ujednolica kolejność). */
    static final class TagCounters {

        private final Map<String, Long> counts = new HashMap<>();

        private static String key(String name, Map<String, String> tags) {
            return name + new TreeMap<>(tags);
        }

        void increment(String name, Map<String, String> tags) {
            counts.merge(key(name, tags), 1L, Long::sum);
        }

        long count(String name, Map<String, String> tags) {
            return counts.getOrDefault(key(name, tags), 0L);
        }

        int seriesCount(String name) {
            // klucz zaczyna się od nazwy, po której następuje "{" (początek mapy tagów)
            return (int) counts.keySet().stream().filter(k -> k.startsWith(name + "{")).count();
        }
    }

    static List<String> solution3(Map<String, Set<String>> observedValues, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit nie może być ujemny");
        }
        return observedValues.entrySet().stream()
                .filter(entry -> entry.getValue().size() > limit)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }
}
