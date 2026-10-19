package s21_capstone.cap02_reservations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import s21_capstone.cap02_reservations.desk.Desk;
import s21_capstone.cap02_reservations.desk.DeskRepository;
import s21_capstone.cap02_reservations.reservation.Reservation;
import s21_capstone.cap02_reservations.reservation.ReservationRepository;
import s21_capstone.cap02_reservations.reservation.ReservationStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy WYCINKA bazy projektu Cap02Reservations ({@code @DataJpaTest}, baza wbudowana o losowej nazwie, migracje
 * z profilu cap02, transakcja wycofywana po każdym teście). Sprawdzają zapytanie o nakładanie się i — co najważniejsze —
 * że ograniczenie w BAZIE zatrzyma podwójną rezerwację, nawet gdy kod ominie sprawdzenie w serwisie.
 */
@DataJpaTest
@ActiveProfiles("cap02")
class Cap02ReservationsRepositoryTest {

    static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 19, 10, 0);

    @Autowired
    ReservationRepository reservations;

    @Autowired
    DeskRepository desks;

    Desk desk2() {
        return desks.findByCode("SERWIS-2").orElseThrow();
    }

    boolean overlaps(String from, String to) {
        return reservations.existsOverlapping(desk2(), ReservationStatus.POTWIERDZONA,
                LocalDateTime.parse(from), LocalDateTime.parse(to));
    }

    @Test
    @DisplayName("4. Nakładanie się [start, koniec): dane startowe SERWIS-2 21.10 9–11 — styk nie jest konfliktem")
    void overlapQuery() {
        assertThat(overlaps("2026-10-21T10:00", "2026-10-21T11:00")).as("w środku").isTrue();
        assertThat(overlaps("2026-10-21T08:00", "2026-10-21T12:00")).as("obejmuje całą").isTrue();
        assertThat(overlaps("2026-10-21T11:00", "2026-10-21T12:00")).as("zaczyna się, gdy tamta się kończy").isFalse();
        assertThat(overlaps("2026-10-22T12:00", "2026-10-22T13:00")).as("rezerwacja ANULOWANA nie blokuje").isFalse();
    }

    @Test
    @DisplayName("4. Ograniczenie w bazie: druga rezerwacja tej samej godziny z POMINIĘCIEM serwisu → DataIntegrityViolationException")
    void primaryKeyStopsDoubleBooking() {
        reservations.saveAndFlush(new Reservation(desk2(), "ola@example.com",
                LocalDateTime.of(2026, 11, 2, 10, 0), 2, CREATED));

        assertThatThrownBy(() -> reservations.saveAndFlush(new Reservation(desk2(), "ewa.lis@example.com",
                LocalDateTime.of(2026, 11, 2, 11, 0), 1, CREATED)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("PK_OCCUPIED_SLOTS");
    }

    @Test
    @DisplayName("4. Anulowanie usuwa wiersze occupied_slots — ta sama godzina znów przechodzi przez ograniczenie")
    void cancellationFreesSlots() {
        Reservation first = reservations.saveAndFlush(new Reservation(desk2(), "ola@example.com",
                LocalDateTime.of(2026, 11, 3, 10, 0), 1, CREATED));
        first.cancel(CREATED);
        reservations.flush();

        Reservation second = reservations.saveAndFlush(new Reservation(desk2(), "ewa.lis@example.com",
                LocalDateTime.of(2026, 11, 3, 10, 0), 1, CREATED));
        assertThat(second.getId()).isNotNull();
    }
}
