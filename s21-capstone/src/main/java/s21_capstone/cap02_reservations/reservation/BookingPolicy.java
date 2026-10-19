package s21_capstone.cap02_reservations.reservation;

import org.springframework.stereotype.Component;
import s21_capstone.cap02_reservations.common.RuleViolationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * Reguły CZASU rezerwacji w jednym miejscu: godziny pracy, pełne godziny, „nie w przeszłości”. „Teraz” pochodzi
 * WYŁĄCZNIE z wstrzykniętego {@link Clock} — test jednostkowy tworzy politykę z {@code Clock.fixed(...)} i sprawdza
 * granice co do sekundy, bez czekania i bez zależności od dzisiejszej daty.
 */
@Component
public class BookingPolicy {

    public static final LocalTime OPENS = LocalTime.of(9, 0);
    public static final LocalTime CLOSES = LocalTime.of(17, 0);

    private final Clock clock;

    public BookingPolicy(Clock clock) {
        this.clock = clock;
    }

    /** „Teraz” w czasie sklepu (strefa z zegara). */
    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * Sprawdza nową rezerwację [start, start + hours). Kolejność ma znaczenie: najpierw format terminu (pełna godzina),
     * potem godziny pracy, na końcu przeszłość — klient dostaje najbardziej konkretny powód.
     *
     * @throws RuleViolationException 422: NOT_FULL_HOUR, OUTSIDE_OPENING_HOURS albo IN_THE_PAST
     */
    public void checkNewReservation(LocalDateTime start, int hours) {
        if (start.getMinute() != 0 || start.getSecond() != 0 || start.getNano() != 0) {
            throw new RuleViolationException("NOT_FULL_HOUR", "Rezerwacja zaczyna się o pełnej godzinie",
                    Map.of("startsAt", start.toString()));
        }
        LocalDateTime end = start.plusHours(hours);
        if (start.toLocalTime().isBefore(OPENS) || end.isAfter(start.toLocalDate().atTime(CLOSES))) {
            throw new RuleViolationException("OUTSIDE_OPENING_HOURS",
                    "Stanowiska są czynne od " + OPENS + " do " + CLOSES,
                    Map.of("startsAt", start.toString(), "endsAt", end.toString()));
        }
        if (!start.isAfter(now())) {
            throw new RuleViolationException("IN_THE_PAST", "Nie można rezerwować terminu, który już minął",
                    Map.of("startsAt", start.toString(), "now", now().toString()));
        }
    }
}
