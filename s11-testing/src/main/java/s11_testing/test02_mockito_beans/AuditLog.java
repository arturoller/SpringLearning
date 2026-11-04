package s11_testing.test02_mockito_beans;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dziennik zdarzeń w pamięci — bean z PRAWDZIWYM zachowaniem i efektem ubocznym (side effect: dopisuje do listy).
 * Dobry kandydat na szpiega ({@code @MockitoSpyBean}): chcesz, żeby działał naprawdę, a jednocześnie sprawdzić,
 * jak go wołano.
 */
@Component
public class AuditLog {

    private final List<String> events = Collections.synchronizedList(new ArrayList<>());

    /** Dopisuje zdarzenie i zwraca, ile zdarzeń jest już w dzienniku. */
    public int record(String event) {
        events.add(event);
        return events.size();
    }

    public List<String> events() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
