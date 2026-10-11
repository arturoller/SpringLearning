package s01_core_ioc.ioc07_lifecycle;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * EventLog = dziennik zdarzeń. Zwykły bean, który zapisuje teksty w kolejności ich dodawania. Dzięki niemu test
 * może SPRAWDZIĆ kolejność zdarzeń cyklu życia (zamiast zgadywać z logów konsoli).
 */
@Component
public class EventLog {

    // lista zsynchronizowana, bo dziennik jest singletonem i może go używać kilka wątków
    private final List<String> events = Collections.synchronizedList(new ArrayList<>());

    public void add(String event) {
        events.add(event);
    }

    /** Kopia zapisanych zdarzeń (kopia, żeby nikt z zewnątrz nie zmienił dziennika). */
    public List<String> events() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }
}
