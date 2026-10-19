package s21_capstone.cap02_reservations.common;

import java.util.Map;

/** Nie ma stanowiska albo rezerwacji → 404. */
public final class NotFoundException extends BookingException {

    public NotFoundException(String what, Object id) {
        super("NOT_FOUND", "Nie znaleziono: " + what + " " + id, Map.of("resource", what, "id", String.valueOf(id)));
    }
}
