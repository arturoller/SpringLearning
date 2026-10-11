package s06_error_handling.solutions.err01_default_errors;

import java.util.Map;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń lekcji Err01DefaultErrors — zajrzyj dopiero po własnej próbie! */
public final class Err01DefaultErrorsSolutions {

    private Err01DefaultErrorsSolutions() {
    }

    static Set<String> solution1() {
        return Set.of("timestamp", "status", "error", "path");
    }

    static Map<String, String> solution2() {
        return Map.of(
                "spring.web.error.include-message", "never",
                "spring.web.error.include-stacktrace", "never",
                "spring.web.error.include-binding-errors", "never",
                "spring.web.error.include-exception", "false");
    }

    static String solution3(Map<String, Object> errorBody) {
        String line = errorBody.get("status") + " " + errorBody.get("error") + ": " + errorBody.get("path");
        return errorBody.containsKey("message") ? line + " (" + errorBody.get("message") + ")" : line;
    }
}
