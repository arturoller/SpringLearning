package s21_capstone.cap02_reservations.common;

import java.util.Map;

/**
 * Dane mają poprawny FORMAT, ale łamią regułę biznesową niezależną od innych rezerwacji: termin w przeszłości,
 * poza godzinami pracy, nie od pełnej godziny → 422 Unprocessable Content („nie da się przetworzyć”).
 * Różnica z 409: tu ponowienie TEGO SAMEGO żądania nigdy się nie uda; przy 409 — może (gdy ktoś anuluje).
 */
public final class RuleViolationException extends BookingException {

    public RuleViolationException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
