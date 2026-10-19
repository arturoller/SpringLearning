package s17_events.evt01_application_events;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dziennik zdarzeń na potrzeby lekcji: słuchacze zapisują tu, że coś zrobili, a test (i {@code main}) czyta
 * kolejność wpisów i nazwę wątku. Lista jest bezpieczna wątkowo ({@code CopyOnWriteArrayList}), bo lekcja 3 pisze do niej z wielu wątków.
 */
@Component
public class EventLog {

    /** Jeden wpis: co się stało i w którym wątku. */
    public record Entry(String what, String thread) {
    }

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public void add(String what) {
        entries.add(new Entry(what, Thread.currentThread().getName()));
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public List<String> whats() {
        return entries.stream().map(Entry::what).toList();
    }

    public void clear() {
        entries.clear();
    }
}
