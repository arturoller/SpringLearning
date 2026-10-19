package s15_actuator_logging.obs05_observations;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Własny handler obserwacji (handler = obsługa zdarzeń cyklu życia obserwacji: start, stop, błąd). Boot sam
 * rejestruje w ObservationRegistry wszystkie beany typu ObservationHandler. Ten handler tylko zapisuje, co widział —
 * w prawdziwej aplikacji handlerem jest np. zapis metryki (robi to Micrometer) albo utworzenie śladu (tracing).
 */
@Component
public class RecordingObservationHandler implements ObservationHandler<Observation.Context> {

    private final List<String> events = new CopyOnWriteArrayList<>();   // lista bezpieczna dla wielu wątków

    @Override
    public boolean supportsContext(Observation.Context context) {   // true = chcę obsługiwać każdą obserwację
        return true;
    }

    @Override
    public void onStart(Observation.Context context) {
        events.add("start:" + context.getName());
    }

    @Override
    public void onError(Observation.Context context) {
        events.add("error:" + context.getName() + ":" + context.getError().getClass().getSimpleName());
    }

    @Override
    public void onStop(Observation.Context context) {
        events.add("stop:" + context.getName());
    }

    /** Kopia zapisanych zdarzeń w kolejności ich wystąpienia. */
    public List<String> events() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
