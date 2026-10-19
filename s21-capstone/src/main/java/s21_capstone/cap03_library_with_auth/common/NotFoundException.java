package s21_capstone.cap03_library_with_auth.common;

import java.util.Map;

/** Nie ma książki, czytelnika albo wypożyczenia → 404. */
public final class NotFoundException extends LibraryException {

    public NotFoundException(String what, Object id) {
        super("NOT_FOUND", "Nie znaleziono: " + what + " " + id, Map.of("resource", what, "id", String.valueOf(id)));
    }
}
