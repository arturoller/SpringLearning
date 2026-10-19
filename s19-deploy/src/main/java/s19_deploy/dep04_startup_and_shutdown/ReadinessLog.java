package s19_deploy.dep04_startup_and_shutdown;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Zapisuje kolejne stany gotowości (readiness) aplikacji — pokazuje, że przy zamykaniu Boot przełącza je na REFUSING_TRAFFIC. */
@Component
public class ReadinessLog {

    private final List<ReadinessState> states = new CopyOnWriteArrayList<>();

    @EventListener   // słuchacz zdarzenia zmiany dostępności; generyczny typ zawęża go do stanów gotowości
    void onReadinessChange(AvailabilityChangeEvent<ReadinessState> event) {
        states.add(event.getState());
    }

    public List<ReadinessState> states() {
        return List.copyOf(states);
    }
}
