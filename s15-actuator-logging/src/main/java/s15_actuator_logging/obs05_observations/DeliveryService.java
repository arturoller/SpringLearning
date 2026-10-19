package s15_actuator_logging.obs05_observations;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wysyłka zamówień opisana JEDNĄ obserwacją (Observation). Z jednego miejsca w kodzie dostajemy: metrykę
 * (czasomierz "deliveries.ship" z tagami), a po dodaniu śledzenia — także ślad (span), i wywołania własnych handlerów.
 */
@Service
public class DeliveryService {

    private static final Set<String> CARRIERS = Set.of("DHL", "INPOST", "POCZTA");   // carrier = przewoźnik

    private final ObservationRegistry registry;   // registry = rejestr obserwacji (bean od Boota)
    private final AtomicInteger sequence = new AtomicInteger();

    public DeliveryService(ObservationRegistry registry) {
        this.registry = registry;
    }

    /** Wysyła zamówienie wybranym przewoźnikiem i zwraca numer przesyłki, np. "TRK-001". */
    public String ship(String orderId, String carrier) {
        return Observation.createNotStarted("deliveries.ship", registry)   // createNotStarted = utwórz niewystartowaną
                // NISKA kardynalność (mało wartości) → trafia do tagów metryki
                .lowCardinalityKeyValue("carrier", carrier)
                // WYSOKA kardynalność (każde zamówienie inne) → NIE trafia do metryk, tylko do śladów i handlerów
                .highCardinalityKeyValue("orderId", orderId)
                .observe(() -> {   // observe = start + wykonanie + stop; wyjątek zostanie zapisany i rzucony dalej
                    if (!CARRIERS.contains(carrier)) {
                        throw new IllegalArgumentException("Nieznany przewoźnik: " + carrier);
                    }
                    return "TRK-%03d".formatted(sequence.incrementAndGet());
                });
    }
}
