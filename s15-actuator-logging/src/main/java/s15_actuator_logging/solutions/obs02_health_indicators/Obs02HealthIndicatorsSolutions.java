package s15_actuator_logging.solutions.obs02_health_indicators;

import s15_actuator_logging.obs02_health_indicators.Obs02HealthIndicators.HealthResult;

import java.util.List;
import java.util.Map;

/** Rozwiązania wzorcowe ćwiczeń lekcji Obs02HealthIndicators — zajrzyj dopiero po własnej próbie! */
public final class Obs02HealthIndicatorsSolutions {

    private static final List<String> ORDER = List.of("DOWN", "OUT_OF_SERVICE", "UP", "UNKNOWN");

    private Obs02HealthIndicatorsSolutions() {
    }

    static int solution1(String status) {
        return switch (status) {
            case "DOWN", "OUT_OF_SERVICE" -> 503;
            case "UP", "UNKNOWN" -> 200;
            default -> throw new IllegalArgumentException("Nieznany status zdrowia: " + status);
        };
    }

    static String solution2(List<String> statuses) {
        // im mniejszy indeks w ORDER, tym gorszy status; pusta lista → UNKNOWN
        return statuses.stream()
                .min((a, b) -> Integer.compare(ORDER.indexOf(a), ORDER.indexOf(b)))
                .orElse("UNKNOWN");
    }

    static HealthResult solution3(long freeBytes, long thresholdBytes) {
        if (freeBytes < 0 || thresholdBytes < 0) {
            throw new IllegalArgumentException("Wartości nie mogą być ujemne");
        }
        if (freeBytes >= thresholdBytes) {
            return new HealthResult("UP", Map.of("free", freeBytes, "threshold", thresholdBytes));
        }
        return new HealthResult("DOWN",
                Map.of("free", freeBytes, "threshold", thresholdBytes, "error", "Za mało miejsca na dysku"));
    }
}
