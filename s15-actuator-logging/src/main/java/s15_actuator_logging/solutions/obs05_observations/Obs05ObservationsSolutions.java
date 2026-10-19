package s15_actuator_logging.solutions.obs05_observations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Supplier;

/** Rozwiązania wzorcowe ćwiczeń lekcji Obs05Observations — zajrzyj dopiero po własnej próbie! */
public final class Obs05ObservationsSolutions {

    private Obs05ObservationsSolutions() {
    }

    static List<String> solution1(Map<String, String> lowCardinality, Map<String, String> highCardinality) {
        TreeSet<String> tags = new TreeSet<>(lowCardinality.keySet());   // TreeSet sortuje alfabetycznie
        tags.add("error");                                               // dokłada go czasomierz
        return List.copyOf(tags);                                        // klucze wysokiej kardynalności pomijamy
    }

    static final class MiniObservation {

        private final List<String> events = new ArrayList<>();

        <T> T observe(String name, Supplier<T> body) {
            events.add("start:" + name);
            try {
                return body.get();
            } catch (RuntimeException e) {
                events.add("error:" + name + ":" + e.getClass().getSimpleName());
                throw e;
            } finally {
                events.add("stop:" + name);
            }
        }

        List<String> events() {
            return List.copyOf(events);
        }
    }

    static boolean solution3(Map<String, Boolean> rules, String observationName) {
        String bestPrefix = null;
        for (String prefix : rules.keySet()) {
            boolean matches = observationName.equals(prefix) || observationName.startsWith(prefix + ".");
            if (matches && (bestPrefix == null || prefix.length() > bestPrefix.length())) {
                bestPrefix = prefix;
            }
        }
        return bestPrefix == null || rules.get(bestPrefix);
    }
}
