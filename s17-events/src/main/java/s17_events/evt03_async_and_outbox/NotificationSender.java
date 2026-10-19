package s17_events.evt03_async_and_outbox;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Udawany zewnętrzny system powiadomień (np. serwer poczty) — w teście nie ma prawdziwej sieci. Można go „wyłączyć”
 * ({@link #setDown(boolean)}), wtedy każda wysyłka rzuca wyjątek. Zapamiętuje każde udane wywołanie ({@link #calls()})
 * oraz zbiór unikalnych identyfikatorów ({@link #delivered()}) — czyli to, co zrobiłby odbiorca odporny na duplikaty.
 */
@Component
public class NotificationSender {

    private final AtomicBoolean down = new AtomicBoolean();
    private final List<String> calls = new CopyOnWriteArrayList<>();
    private final Set<String> delivered = ConcurrentHashMap.newKeySet();

    public void setDown(boolean down) {
        this.down.set(down);
    }

    /** Wysyłka bezpośrednia ze słuchacza zdarzeń (sekcja 4 lekcji). */
    public void sendDirect(String orderId) {
        failIfDown();
        calls.add("zamówienie " + orderId);
        delivered.add("zamówienie " + orderId);
    }

    /** Wysyłka wiadomości z outboxa; odbiorca rozpoznaje duplikaty po id wiadomości. */
    public void send(OutboxMessage message) {
        failIfDown();
        calls.add("wiadomość " + message.id());
        delivered.add("wiadomość " + message.id());
    }

    private void failIfDown() {
        if (down.get()) {
            throw new IllegalStateException("Serwer poczty niedostępny");
        }
    }

    public List<String> calls() {
        return List.copyOf(calls);
    }

    public Set<String> delivered() {
        return Set.copyOf(delivered);
    }

    public void reset() {
        down.set(false);
        calls.clear();
        delivered.clear();
    }
}
