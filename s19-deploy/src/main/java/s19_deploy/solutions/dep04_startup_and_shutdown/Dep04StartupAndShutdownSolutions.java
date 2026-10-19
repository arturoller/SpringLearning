package s19_deploy.solutions.dep04_startup_and_shutdown;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Dep04StartupAndShutdown — zajrzyj dopiero po własnej próbie! */
public final class Dep04StartupAndShutdownSolutions {

    private Dep04StartupAndShutdownSolutions() {
    }

    static boolean solution1(String livenessStatus, String readinessStatus) {
        return "UP".equals(readinessStatus);
    }

    static String solution2(String livenessStatus, String readinessStatus) {
        if (!"UP".equals(livenessStatus)) {
            return "RESTART";
        }
        if (!"UP".equals(readinessStatus)) {
            return "WYLACZ_Z_BALANSERA";
        }
        return "ZOSTAW";
    }

    static List<String> solution3(Duration springTimeoutPerPhase, int preStopSeconds, int terminationGracePeriodSeconds) {
        List<String> problems = new ArrayList<>();
        if (preStopSeconds + springTimeoutPerPhase.getSeconds() >= terminationGracePeriodSeconds) {
            problems.add("ZA_KROTKI_CZAS_ZAMKNIECIA");
        }
        if (springTimeoutPerPhase.isZero() || springTimeoutPerPhase.isNegative()) {
            problems.add("ZEROWY_LIMIT");
        }
        return problems;
    }
}
