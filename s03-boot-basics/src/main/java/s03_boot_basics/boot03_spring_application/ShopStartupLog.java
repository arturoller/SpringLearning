package s03_boot_basics.boot03_spring_application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dziennik startu sklepu: runnery zapisują tu, co zrobiły, a test czyta wpisy (dowód, że kod się wykonał).
 * <p>
 * Każdy wpis jest też publikowany jako zdarzenie (event = zdarzenie) — dzięki temu odbiornik z sekcji 4 widzi
 * wpisy runnerów w jednej kolejności ze zdarzeniami samego Boota.
 */
@Component
public class ShopStartupLog {

    private final List<String> entries = new CopyOnWriteArrayList<>();
    private final ApplicationEventPublisher publisher; // publisher = wydawca (rozsyła zdarzenia do odbiorników)

    public ShopStartupLog(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void add(String entry) {
        entries.add(entry);
        publisher.publishEvent(entry); // zwykły obiekt jako zdarzenie: Spring opakuje go w PayloadApplicationEvent
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }
}
