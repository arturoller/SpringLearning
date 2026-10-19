package s21_capstone.cap02_reservations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import s21_capstone.cap02_reservations.common.ConflictException;
import s21_capstone.cap02_reservations.common.RuleViolationException;
import s21_capstone.cap02_reservations.desk.Desk;
import s21_capstone.cap02_reservations.reservation.BookingPolicy;
import s21_capstone.cap02_reservations.reservation.Reservation;
import s21_capstone.cap02_reservations.reservation.ReservationStatus;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy JEDNOSTKOWE reguł czasu projektu Cap02Reservations — bez Springa. BookingPolicy dostaje zegar zatrzymany na
 * 2026-10-19 10:00 (czas polski), więc granice sprawdzamy co do minuty, a wynik nie zależy od dnia uruchomienia testu.
 */
class Cap02ReservationsUnitTest {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 10, 0);
    static final Clock CLOCK = Clock.fixed(NOW.atZone(Cap02Config.ZONE).toInstant(), Cap02Config.ZONE);
    final BookingPolicy policy = new BookingPolicy(CLOCK);

    static Reservation reservation(LocalDateTime start, int hours) {
        return new Reservation(new Desk("SERWIS-1", "Stanowisko 1"), "jan@example.com", start, hours, NOW);
    }

    @ParameterizedTest(name = "{0} na {1} h → {2}")
    @CsvSource({
            "2026-10-19T11:00, 1, OK",
            "2026-10-20T09:00, 3, OK",
            "2026-10-20T15:00, 2, OK",                      // kończy się dokładnie o 17:00
            "2026-10-20T16:00, 2, OUTSIDE_OPENING_HOURS",   // kończyłaby się o 18:00
            "2026-10-20T08:00, 1, OUTSIDE_OPENING_HOURS",
            "2026-10-20T10:30, 1, NOT_FULL_HOUR",
            "2026-10-19T10:00, 1, IN_THE_PAST",             // „teraz” to już nie przyszłość
            "2026-10-19T09:00, 1, IN_THE_PAST"})
    @DisplayName("3. BookingPolicy: pełne godziny, godziny pracy 9–17 i „nie w przeszłości” liczone z Clock")
    void bookingPolicy(LocalDateTime start, int hours, String expected) {
        if (expected.equals("OK")) {
            assertThatCode(() -> policy.checkNewReservation(start, hours)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> policy.checkNewReservation(start, hours))
                    .isInstanceOfSatisfying(RuleViolationException.class, e -> assertThat(e.code()).isEqualTo(expected));
        }
    }

    @Test
    @DisplayName("3. Anulowanie najpóźniej 24 h przed początkiem: dokładnie na granicy wolno, minutę później — 409")
    void cancellationDeadline() {
        Reservation tomorrow = reservation(LocalDateTime.of(2026, 10, 20, 10, 0), 1);
        assertThat(tomorrow.cancellableUntil()).isEqualTo(NOW);
        tomorrow.cancel(NOW);   // dokładnie 24 h przed — jeszcze wolno
        assertThat(tomorrow.getStatus()).isEqualTo(ReservationStatus.ANULOWANA);
        assertThat(tomorrow.getOccupiedSlots()).as("godzina zwolniona").isEmpty();

        Reservation soon = reservation(LocalDateTime.of(2026, 10, 20, 9, 0), 1);
        assertThatThrownBy(() -> soon.cancel(NOW))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo("TOO_LATE_TO_CANCEL"));
        assertThat(soon.getStatus()).isEqualTo(ReservationStatus.POTWIERDZONA);
    }

    @Test
    @DisplayName("3. Drugie anulowanie → 409 ALREADY_CANCELLED; rezerwacja 3 h zajmuje 3 kolejne godziny")
    void secondCancellationAndSlots() {
        Reservation reservation = reservation(LocalDateTime.of(2026, 10, 22, 12, 0), 3);
        assertThat(reservation.getOccupiedSlots()).hasSize(3);
        assertThat(reservation.covers(LocalDateTime.of(2026, 10, 22, 14, 0))).isTrue();
        assertThat(reservation.covers(LocalDateTime.of(2026, 10, 22, 15, 0))).as("koniec nie należy").isFalse();

        reservation.cancel(NOW);
        assertThatThrownBy(() -> reservation.cancel(NOW))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo("ALREADY_CANCELLED"));
        assertThat(reservation.covers(LocalDateTime.of(2026, 10, 22, 12, 0))).as("anulowana nic nie zajmuje").isFalse();
    }
}
