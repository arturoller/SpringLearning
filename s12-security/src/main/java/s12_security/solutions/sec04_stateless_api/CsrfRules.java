package s12_security.solutions.sec04_stateless_api;

import java.util.Locale;
import java.util.Set;

/** Rozwiązanie wzorcowe ćwiczenia 1 lekcji Sec04StatelessApi (ta sama lista co w {@code CsrfFilter} Spring Security). */
public final class CsrfRules {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "TRACE", "OPTIONS");

    private CsrfRules() {
    }

    public static boolean requiresCsrf(String httpMethod) {
        return !SAFE_METHODS.contains(httpMethod.toUpperCase(Locale.ROOT));
    }
}
