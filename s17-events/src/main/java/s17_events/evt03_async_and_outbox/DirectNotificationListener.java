package s17_events.evt03_async_and_outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Słuchacz wysyłający powiadomienie BEZPOŚREDNIO po commicie. Jeśli wysyłka się nie uda (serwer poczty padł, aplikacja
 * została zabita), zdarzenie przepada — nic go nie zapisało i nic go nie ponowi (sekcja 4 lekcji).
 */
@Component
public class DirectNotificationListener {

    private final NotificationSender sender;

    public DirectNotificationListener(NotificationSender sender) {
        this.sender = sender;
    }

    @TransactionalEventListener
    public void onOrderPlaced(OrderPlaced event) {
        sender.sendDirect(event.orderId());
    }
}
