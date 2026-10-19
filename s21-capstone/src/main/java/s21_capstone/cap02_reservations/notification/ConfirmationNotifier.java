package s21_capstone.cap02_reservations.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import s21_capstone.cap02_reservations.reservation.BookingPolicy;
import s21_capstone.cap02_reservations.reservation.ReservationConfirmed;

import java.time.format.DateTimeFormatter;

/**
 * Słuchacz zdarzenia „potwierdzono” — wysyła (zapisuje) potwierdzenie DOPIERO PO ZATWIERDZENIU transakcji rezerwacji.
 *
 * <ul>
 *   <li>{@code @TransactionalEventListener(phase = AFTER_COMMIT)} (słuchacz transakcyjny, faza „po commicie”):
 *       gdy rezerwacja się wycofa (rollback), potwierdzenie NIE wyjdzie. Zwykły {@code @EventListener} wysłałby je
 *       od razu w trakcie transakcji — także dla rezerwacji, której za chwilę nie będzie;</li>
 *   <li>{@code @Transactional(propagation = REQUIRES_NEW)}: po commicie transakcja rezerwacji jest ZAKOŃCZONA, choć
 *       jej zasoby są jeszcze przypięte do wątku. Zapis bez nowej transakcji dołączyłby do tej zakończonej — i nigdy nie
 *       zostałby zatwierdzony (Cap02ReservationsEventsTest to pokazuje). Spring 6.1+ pilnuje tego przy starcie: na
 *       takim słuchaczu dopuszcza tylko {@code @Transactional} z REQUIRES_NEW albo NOT_SUPPORTED.</li>
 * </ul>
 */
@Component
public class ConfirmationNotifier {

    private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificationRepository notifications;
    private final BookingPolicy policy;

    public ConfirmationNotifier(NotificationRepository notifications, BookingPolicy policy) {
        this.notifications = notifications;
        this.policy = policy;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onConfirmed(ReservationConfirmed event) {
        String message = "Potwierdzamy rezerwację stanowiska " + event.deskCode() + ": "
                + event.startsAt().format(DAY_TIME) + "–" + event.endsAt().format(TIME);
        notifications.save(new Notification(event.reservationId(), event.customerEmail(), message, policy.now()));
    }
}
