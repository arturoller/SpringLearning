package s21_capstone.cap02_reservations;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import s21_capstone.cap02_reservations.common.ConflictException;
import s21_capstone.cap02_reservations.common.NotFoundException;
import s21_capstone.cap02_reservations.notification.Notification;
import s21_capstone.cap02_reservations.notification.NotificationRepository;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.NewReservationRequest;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.ReservationResponse;
import s21_capstone.cap02_reservations.reservation.ReservationRepository;
import s21_capstone.cap02_reservations.reservation.ReservationService;
import s21_capstone.cap02_reservations.reservation.ReservationStatus;
import s21_capstone.cap02_reservations.waitlist.WaitlistRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy ĆWICZEŃ projektu Cap02Reservations. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Własna baza ({@code jdbc:h2:mem:cap02-cwiczenia}) czyszczona przed każdym testem (flyway clean + migrate).
 * Zegar z profilu cap02: 2026-10-19 10:00. Dane startowe — V2__sample_data.sql (rezerwacje 1–5).
 */
@Tag("cwiczenie")
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:cap02-cwiczenia", "spring.flyway.clean-disabled=false"})
@ActiveProfiles("cap02")
class Cap02ReservationsExercisesTest {

    @Autowired
    Cap02Exercises exercises;

    @Autowired
    ReservationService reservationService;

    @Autowired
    ReservationRepository reservations;

    @Autowired
    NotificationRepository notifications;

    @Autowired
    WaitlistRepository waitlist;

    @Autowired
    Flyway flyway;

    @BeforeEach
    void resetDatabase() {
        flyway.clean();
        flyway.migrate();
    }

    static NewReservationRequest request(String desk, String startsAt, String email) {
        return new NewReservationRequest(desk, LocalDateTime.parse(startsAt), 1, email);
    }

    List<String> messagesFor(long reservationId) {
        return notifications.findByReservationIdOrderById(reservationId).stream().map(Notification::getMessage).toList();
    }

    @Test
    @DisplayName("Ćwiczenie 1: najwyżej 2 aktywne, przyszłe rezerwacje na klienta (email bez względu na wielkość liter)")
    void exercise1() {
        // Jan ma już jedną (20.10 14:00) → jeszcze jedna wolno, trzecia nie
        assertThat(exercises.reserveWithinLimit(request("SERWIS-2", "2026-10-23T10:00", "jan@example.com")).id())
                .isNotNull();
        assertThatThrownBy(() -> exercises.reserveWithinLimit(request("SERWIS-2", "2026-10-23T12:00", "JAN@example.com")))
                .isInstanceOfSatisfying(ConflictException.class, e -> {
                    assertThat(e.code()).isEqualTo("RESERVATION_LIMIT");
                    assertThat(e.details()).containsEntry("limit", 2);
                });
        // Adam ma tylko minioną (18.10), Ewa tylko anulowaną — nie liczą się
        exercises.reserveWithinLimit(request("SERWIS-2", "2026-10-24T10:00", "adam.mazur@example.com"));
        exercises.reserveWithinLimit(request("SERWIS-2", "2026-10-24T11:00", "adam.mazur@example.com"));
        exercises.reserveWithinLimit(request("SERWIS-2", "2026-10-24T12:00", "ewa.lis@example.com"));
        exercises.reserveWithinLimit(request("SERWIS-2", "2026-10-24T13:00", "ewa.lis@example.com"));
        assertThatThrownBy(() -> exercises.reserveWithinLimit(request("SERWIS-2", "2026-10-24T14:00", "ewa.lis@example.com")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 2: przypomnienia na najbliższe 24 h — tylko raz dla każdej rezerwacji")
    void exercise2() {
        assertThat(exercises.sendReminders()).as("tylko rezerwacja 2 (dziś 15:00)").isEqualTo(1);
        assertThat(messagesFor(2L)).anyMatch(message -> message.startsWith("Przypomnienie"));
        assertThat(reservations.findById(2L).orElseThrow().getReminderSentAt())
                .isEqualTo(LocalDateTime.of(2026, 10, 19, 10, 0));
        assertThat(exercises.sendReminders()).as("drugi raz — nic nowego").isZero();

        ReservationResponse early = reservationService.reserve(request("SERWIS-2", "2026-10-20T09:00", "ola@example.com"));
        assertThat(exercises.sendReminders()).as("nowa rezerwacja jutro 9:00 mieści się w 24 h").isEqualTo(1);
        assertThat(messagesFor(early.id())).anyMatch(message -> message.startsWith("Przypomnienie"));
        assertThat(messagesFor(1L)).as("rezerwacja 1 (jutro 14:00) jest poza oknem 24 h").isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: lista oczekujących — tylko na zajętą godzinę; anulowanie awansuje pierwszego z kolejki")
    void exercise3() {
        LocalDateTime janSlot = LocalDateTime.of(2026, 10, 20, 14, 0);   // rezerwacja 1 (Jan)
        assertThat(exercises.joinWaitlist("SERWIS-1", janSlot, "ola@example.com")).isEqualTo(1);
        assertThat(exercises.joinWaitlist("SERWIS-1", janSlot, "ewa.lis@example.com")).isEqualTo(2);
        assertThatThrownBy(() -> exercises.joinWaitlist("SERWIS-1", LocalDateTime.of(2026, 10, 20, 10, 0), "ola@example.com"))
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("SLOT_FREE"));
        assertThatThrownBy(() -> exercises.joinWaitlist("SERWIS-9", janSlot, "ola@example.com"))
                .isInstanceOf(NotFoundException.class);

        List<ReservationResponse> promoted = exercises.cancelAndPromote(1L);

        assertThat(promoted).singleElement().satisfies(r -> {
            assertThat(r.customerEmail()).isEqualTo("ola@example.com");
            assertThat(r.startsAt()).isEqualTo(janSlot);
            assertThat(messagesFor(r.id())).as("potwierdzenie po commicie").hasSize(1);
        });
        assertThat(reservations.findById(1L).orElseThrow().getStatus()).isEqualTo(ReservationStatus.ANULOWANA);
        assertThat(waitlist.findAll()).extracting(entry -> entry.getCustomerEmail()).containsExactly("ewa.lis@example.com");

        // za późno na anulowanie → nikt nie awansuje, kolejka bez zmian
        LocalDateTime zofiaSlot = LocalDateTime.of(2026, 10, 19, 15, 0);   // rezerwacja 2 (dziś 15:00)
        exercises.joinWaitlist("SERWIS-1", zofiaSlot, "marek@example.com");
        assertThatThrownBy(() -> exercises.cancelAndPromote(2L))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo("TOO_LATE_TO_CANCEL"));
        assertThat(waitlist.findAll()).hasSize(2);
    }
}
