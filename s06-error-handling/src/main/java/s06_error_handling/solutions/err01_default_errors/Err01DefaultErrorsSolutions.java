package s06_error_handling.solutions.err01_default_errors;

import s06_error_handling.err01_default_errors.Err01DefaultErrors.IncludeMode;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Rozwiązania wzorcowe ćwiczeń lekcji Err01DefaultErrors — zajrzyj dopiero po własnej próbie! */
public final class Err01DefaultErrorsSolutions {

    private Err01DefaultErrorsSolutions() {
    }

    static String solution1(int status) {
        return switch (status / 100) {
            case 2 -> "sukces";
            case 3 -> "przekierowanie";
            case 4 -> "błąd klienta";
            case 5 -> "błąd serwera";
            default -> "nieznany";
        };
    }

    static Map<String, Object> solution2(int status, String reason, String path, Instant timestamp) {
        return solution3(status, reason, path, timestamp, IncludeMode.NEVER, null, null);
    }

    static Map<String, Object> solution3(int status, String reason, String path, Instant timestamp,
                                         IncludeMode mode, String messageParam, String message) {
        boolean includeMessage = switch (mode) {
            case NEVER -> false;
            case ALWAYS -> true;
            case ON_PARAM -> messageParam != null && !"false".equalsIgnoreCase(messageParam);
        };
        Map<String, Object> body = new LinkedHashMap<>();   // zachowuje kolejność wstawiania
        body.put("timestamp", timestamp.toString());
        body.put("status", status);
        body.put("error", reason);
        if (includeMessage) {
            body.put("message", message);
        }
        body.put("path", path);
        return body;
    }
}
