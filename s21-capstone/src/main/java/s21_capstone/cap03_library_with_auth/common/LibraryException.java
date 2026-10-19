package s21_capstone.cap03_library_with_auth.common;

import java.util.Map;

/** Wyjątki domeny biblioteki (sealed): NotFound → 404, Conflict → 409. Status dobiera LibraryProblemAdvice. */
public abstract sealed class LibraryException extends RuntimeException permits NotFoundException, ConflictException {

    private final String code;
    private final Map<String, Object> details;

    protected LibraryException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = Map.copyOf(details);
    }

    public String code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
