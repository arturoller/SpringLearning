package s06_error_handling.solutions.err05_custom_exceptions;

import s06_error_handling.err05_custom_exceptions.Err05CustomExceptions.ErrorKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń lekcji Err05CustomExceptions — zajrzyj dopiero po własnej próbie! */
public final class Err05CustomExceptionsSolutions {

    private Err05CustomExceptionsSolutions() {
    }

    static int solution1(ErrorKind kind) {
        return switch (kind) {
            case MALFORMED_JSON, FIELD_FORMAT -> 400;
            case RESOURCE_MISSING_IN_URL -> 404;
            case STATE_CONFLICT -> 409;
            case REFERENCE_MISSING_IN_BODY, BUSINESS_RULE -> 422;
            case DEPENDENCY_DOWN -> 503;
            case BUG -> 500;
        };
    }

    static Map<String, String> solution2(int status, String internalMessage, String errorId) {
        Map<String, String> body = new LinkedHashMap<>();
        if (status >= 400 && status < 500) {
            body.put("detail", internalMessage);
        } else if (status >= 500) {
            body.put("detail", "Wystąpił błąd wewnętrzny.");
            body.put("errorId", errorId);
        } else {
            throw new IllegalArgumentException("To nie jest status błędu: " + status);
        }
        return body;
    }

    static List<String> solution3(Throwable error) {
        List<String> chain = new ArrayList<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());   // porównanie po tożsamości obiektu
        for (Throwable current = error; current != null && visited.add(current); current = current.getCause()) {
            String name = current.getClass().getSimpleName();
            chain.add(current.getMessage() == null ? name : name + ": " + current.getMessage());
        }
        return chain;
    }
}
