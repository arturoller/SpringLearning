package s03_boot_basics.boot03_spring_application;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dziennik startu aplikacji (journal = dziennik): zapisuje po kolei, co się wydarzyło — zdarzenia widziane przez
 * {@code @EventListener} oraz wpisy runnerów i innych beanów. Testy czytają z niego kolejność.
 *
 * <p>{@code @EventListener} na metodzie z parametrem {@link ApplicationEvent} = „słuchaj WSZYSTKICH zdarzeń
 * aplikacji”. Ale bean jest słuchaczem dopiero wtedy, gdy kontekst go utworzy — zdarzeń sprzed tej chwili
 * nie zobaczy (PUŁAPKA z lekcji, sekcja 6).</p>
 */
@Component
public class StartupJournal {

    private final List<String> entries = Collections.synchronizedList(new ArrayList<>());

    public void record(String entry) {
        entries.add(entry);
    }

    @EventListener
    void onAnyEvent(ApplicationEvent event) {
        record("zdarzenie: " + event.getClass().getSimpleName());
    }

    /** Kopia wpisów (niezmienna lista) — w kolejności dodania. */
    public List<String> entries() {
        synchronized (entries) {
            return List.copyOf(entries);
        }
    }
}
