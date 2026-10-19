package s21_capstone.cap02_reservations.common;

import java.util.Map;

/** Żądanie kłóci się ze STANEM danych: termin zajęty, za późno na anulowanie, już anulowana → 409. */
public final class ConflictException extends BookingException {

    public ConflictException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
