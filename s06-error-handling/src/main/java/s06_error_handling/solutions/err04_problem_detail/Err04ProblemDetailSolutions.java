package s06_error_handling.solutions.err04_problem_detail;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń lekcji Err04ProblemDetail — zajrzyj dopiero po własnej próbie! */
public final class Err04ProblemDetailSolutions {

    private static final Set<String> RESERVED = Set.of("type", "title", "status", "detail", "instance");

    private Err04ProblemDetailSolutions() {
    }

    static boolean solution1(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        return lower.startsWith("application/problem+json") || lower.startsWith("application/problem+xml");
    }

    static Map<String, Object> solution2(int status, String title, String detail, String instance, Map<String, Object> extensions) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        if (title != null) {
            body.put("title", title);
        }
        if (detail != null) {
            body.put("detail", detail);
        }
        if (instance != null) {
            body.put("instance", instance);
        }
        extensions.forEach((name, value) -> {
            if (!RESERVED.contains(name)) {
                body.put(name, value);
            }
        });
        return body;
    }

    static String solution3(Map<String, Map<String, String>> bundles, String language, String key, Object... args) {
        String text = null;
        Map<String, String> specific = bundles.get(language);
        if (specific != null) {
            text = specific.get(key);
        }
        if (text == null) {
            Map<String, String> defaults = bundles.get("");
            if (defaults != null) {
                text = defaults.get(key);
            }
        }
        if (text == null) {
            return null;
        }
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return text;
    }
}
