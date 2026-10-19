package s17_events.evt03_async_and_outbox;

import org.springframework.stereotype.Component;

/**
 * Przekaźnik outboxa (relay = przekaźnik): czyta niewysłane wiadomości i wysyła je do odbiorcy. Wiadomość oznacza jako
 * wysłaną DOPIERO po udanej wysyłce, a przy błędzie zwiększa licznik prób i zostawia ją na następny raz.
 * W prawdziwej aplikacji {@link #sendPending()} wołałby harmonogram ({@code @Scheduled} — dział s16); w lekcji wołamy go
 * jawnie, żeby testy były deterministyczne.
 */
@Component
public class OutboxRelay {

    private final OrderRepository repository;
    private final NotificationSender sender;

    public OutboxRelay(OrderRepository repository, NotificationSender sender) {
        this.repository = repository;
        this.sender = sender;
    }

    /** Wysyła niewysłane wiadomości w kolejności zapisu; zwraca liczbę wysłanych w tym przebiegu. */
    public int sendPending() {
        int sent = 0;
        for (OutboxMessage message : repository.findUnsent()) {
            try {
                sender.send(message);
                repository.markSent(message.id());
                sent++;
            } catch (RuntimeException e) {
                repository.incrementAttempts(message.id());   // zostaje w skrzynce — spróbujemy przy następnym przebiegu
            }
        }
        return sent;
    }
}
