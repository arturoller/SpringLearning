package s15_actuator_logging.obs03_metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serwis zamówień z trzema rodzajami metryk Micrometera:
 * licznik (Counter) — ile zamówień złożono; czasomierz (Timer) — ile razy i jak długo trwało przetwarzanie;
 * wskaźnik (Gauge) — ile zamówień jest teraz otwartych (wartość, która rośnie i maleje).
 * MeterRegistry (rejestr metryk) dostajemy przez konstruktor — Boot tworzy go sam, gdy jest starter actuator.
 */
@Service   // service = usługa (bean warstwy logiki)
public class OrderService {

    private final MeterRegistry registry;
    private final AtomicInteger sequence = new AtomicInteger();      // numer kolejnego zamówienia
    private final AtomicInteger openOrders = new AtomicInteger();    // zamówienia złożone, a jeszcze niewysłane

    public OrderService(MeterRegistry registry) {
        this.registry = registry;
        // Gauge czyta wartość z obiektu openOrders przy KAŻDYM odczycie metryki. Obiekt musi żyć co najmniej tak długo
        // jak metryka — Micrometer trzyma go tylko przez słabe odwołanie, więc pole w tym serwisie go „przypina”.
        Gauge.builder("orders.open", openOrders, AtomicInteger::get)
                .description("Zamówienia złożone, a jeszcze niewysłane")   // description = opis
                .register(registry);
    }

    /** Składa zamówienie i zwraca jego numer, np. "ZAM-001". */
    public String placeOrder(NewOrder order) {
        // Timer: record(...) uruchamia kod i zlicza jedno wykonanie (oraz czas, którego w testach NIE sprawdzamy)
        Timer timer = Timer.builder("orders.processing").register(registry);
        String id = timer.record(() -> "ZAM-%03d".formatted(sequence.incrementAndGet()));

        // Counter z tagiem channel: ta sama nazwa + inna wartość tagu = osobna, niezależna seria
        Counter.builder("orders.created")
                .tag("channel", order.channel())    // tag = etykieta wymiaru (kanał sprzedaży)
                .register(registry)
                .increment();
        openOrders.incrementAndGet();
        return id;
    }

    /** Wysyła zamówienie — ubywa jedno otwarte. */
    public void shipOrder() {
        openOrders.decrementAndGet();
    }
}
