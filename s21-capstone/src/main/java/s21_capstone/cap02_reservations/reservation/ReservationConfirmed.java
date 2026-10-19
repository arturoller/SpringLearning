package s21_capstone.cap02_reservations.reservation;

import java.time.LocalDateTime;

/**
 * Zdarzenie (event) „rezerwacja potwierdzona”. Niezmienny rekord z danymi, a nie encja: słuchacz działa PO zamknięciu
 * transakcji, więc nie powinien dotykać leniwych relacji encji. Serwis rezerwacji nie wie, kto słucha — powiadomienia
 * (pakiet notification) zależą od rezerwacji, a nie odwrotnie (s17_events/Evt01ApplicationEvents).
 */
public record ReservationConfirmed(Long reservationId, String customerEmail, String deskCode,
                                   LocalDateTime startsAt, LocalDateTime endsAt) {
}
