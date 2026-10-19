package s21_capstone.cap03_library_with_auth.common;

import java.util.Map;

/** Reguła biblioteki nie pozwala na operację w obecnym stanie (książka wypożyczona, zaległości, limit) → 409. */
public final class ConflictException extends LibraryException {

    public ConflictException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
