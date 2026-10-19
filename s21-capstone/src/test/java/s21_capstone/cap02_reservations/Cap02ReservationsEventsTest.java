package s21_capstone.cap02_reservations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import s21_capstone.cap02_reservations.notification.Notification;
import s21_capstone.cap02_reservations.notification.NotificationRepository;
import s21_capstone.cap02_reservations.reservation.ReservationConfirmed;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.NewReservationRequest;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.ReservationResponse;
import s21_capstone.cap02_reservations.reservation.ReservationRepository;
import s21_capstone.cap02_reservations.reservation.ReservationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Cap02Reservations: zdarzenie „potwierdzono” i słuchacz PO ZATWIERDZENIU transakcji.
 *
 * <p>{@code @TestConfiguration} dokłada dwóch słuchaczy TYLKO na potrzeby testu (porównanie z prawdziwym
 * ConfirmationNotifier):</p>
 * <ul>
 *   <li>{@code PlainListener} — zwykły {@code @EventListener}: dostaje zdarzenie natychmiast, w trakcie transakcji;</li>
 *   <li>{@code ListenerWithoutNewTransaction} — {@code @TransactionalEventListener} BEZ {@code REQUIRES_NEW}, który
 *       też zapisuje Notification (z dopiskiem „[bez REQUIRES_NEW]”) — żeby zobaczyć, co dzieje się z tym zapisem.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("cap02")
class Cap02ReservationsEventsTest {

    static final String LOST_MARK = "[bez REQUIRES_NEW]";

    @TestConfiguration
    static class TestListeners {

        @Bean
        PlainListener plainListener() {
            return new PlainListener();
        }

        @Bean
        ListenerWithoutNewTransaction listenerWithoutNewTransaction(NotificationRepository notifications) {
            return new ListenerWithoutNewTransaction(notifications);
        }
    }

    /** Zwykły słuchacz — zapamiętuje id rezerwacji z każdego zdarzenia. */
    static class PlainListener {
        final List<Long> received = new CopyOnWriteArrayList<>();

        @EventListener
        void on(ReservationConfirmed event) {
            received.add(event.reservationId());
        }
    }

    /** Słuchacz po commicie, ale BEZ nowej transakcji — jego zapis dołącza do transakcji, która już się zakończyła. */
    static class ListenerWithoutNewTransaction {
        final NotificationRepository notifications;

        ListenerWithoutNewTransaction(NotificationRepository notifications) {
            this.notifications = notifications;
        }

        @TransactionalEventListener
        void on(ReservationConfirmed event) {
            notifications.save(new Notification(event.reservationId(), event.customerEmail(), LOST_MARK,
                    LocalDateTime.of(2026, 10, 19, 10, 0)));
        }
    }

    @Autowired
    ReservationService reservationService;

    @Autowired
    ReservationRepository reservations;

    @Autowired
    NotificationRepository notifications;

    @Autowired
    PlainListener plainListener;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    ApplicationEventPublisher events;

    List<String> messagesFor(long reservationId) {
        return notifications.findByReservationIdOrderById(reservationId).stream().map(Notification::getMessage).toList();
    }

    static NewReservationRequest request(int day, int hour) {
        return new NewReservationRequest("SERWIS-1", LocalDateTime.of(2026, 11, day, hour, 0), 1, "ewa.lis@example.com");
    }

    @Test
    @DisplayName("5. Po commicie: ConfirmationNotifier zapisał potwierdzenie z terminem")
    void confirmationAfterCommit() {
        ReservationResponse reservation = reservationService.reserve(request(9, 10));

        assertThat(messagesFor(reservation.id()))
                .contains("Potwierdzamy rezerwację stanowiska SERWIS-1: 2026-11-09 10:00–11:00");
    }

    @Test
    @DisplayName("5. Rollback: rezerwacji nie ma i potwierdzenia nie ma — ale zwykły @EventListener zdarzenie DOSTAŁ")
    void rollbackMeansNoConfirmation() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        ReservationResponse reservation = outer.execute(status -> {
            ReservationResponse created = reservationService.reserve(request(10, 10));
            status.setRollbackOnly();   // np. dalszy krok procesu się nie udał — wszystko wycofujemy
            return created;
        });

        assertThat(reservations.findById(reservation.id())).as("rezerwacja wycofana").isEmpty();
        assertThat(messagesFor(reservation.id())).as("AFTER_COMMIT nie ruszył").isEmpty();
        assertThat(plainListener.received).as("zwykły słuchacz „wysłałby maila” o nieistniejącej rezerwacji")
                .contains(reservation.id());
    }

    @Test
    @DisplayName("5. PUŁAPKA: zapis w słuchaczu AFTER_COMMIT bez REQUIRES_NEW po cichu przepada (brak błędu, brak wiersza)")
    void saveWithoutRequiresNewIsLost() {
        ReservationResponse reservation = reservationService.reserve(request(11, 10));

        assertThat(messagesFor(reservation.id()))
                .as("jest potwierdzenie z REQUIRES_NEW, a zapisu „" + LOST_MARK + "” nie ma")
                .hasSize(1)
                .doesNotContain(LOST_MARK);
    }

    @Test
    @DisplayName("5. PUŁAPKA: zdarzenie opublikowane BEZ transakcji — @TransactionalEventListener w ogóle nie rusza")
    void noTransactionNoListener() {
        events.publishEvent(new ReservationConfirmed(999_001L, "ewa.lis@example.com", "SERWIS-1",
                LocalDateTime.of(2026, 11, 12, 10, 0), LocalDateTime.of(2026, 11, 12, 11, 0)));

        assertThat(messagesFor(999_001L)).isEmpty();
        assertThat(plainListener.received).as("zwykły słuchacz działa zawsze").contains(999_001L);
    }
}
