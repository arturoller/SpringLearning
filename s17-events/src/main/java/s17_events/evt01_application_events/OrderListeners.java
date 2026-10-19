package s17_events.evt01_application_events;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Słuchacze (listener = słuchacz) zdarzeń lekcji: magazyn, punkty, powiadomienie i kilka wariantów pokazujących
 * kolejność, warunek, zdarzenie zwrócone ze słuchacza i wyjątek.
 */
@Component
public class OrderListeners {

    private final EventLog log;

    public OrderListeners(EventLog log) {
        this.log = log;
    }

    @Order(1)
    @EventListener
    public void reserveStock(OrderPlaced event) {
        log.add("magazyn:" + event.orderId());
    }

    /** Zwraca nowe zdarzenie — Spring opublikuje je zaraz po zakończeniu tej metody. */
    @Order(2)
    @EventListener
    public PointsGranted grantPoints(OrderPlaced event) {
        int points = event.totalPln().intValue() / 10;   // 1 punkt za każde pełne 10 zł
        log.add("punkty:" + event.orderId());
        return new PointsGranted(event.customer(), points);
    }

    @Order(3)
    @EventListener
    public void notifyCustomer(OrderPlaced event) {
        log.add("powiadomienie:" + event.orderId());
    }

    /** Warunek (SpEL): metoda w ogóle nie jest wywoływana dla zamówień poniżej 1000 zł. */
    @EventListener(condition = "#event.totalPln >= 1000")
    public void flagBigOrder(OrderPlaced event) {
        log.add("duże-zamówienie:" + event.orderId());
    }

    @EventListener
    public void onPointsGranted(PointsGranted event) {
        log.add("punkty-zapisane:" + event.customer() + "=" + event.points());
    }

    @Order(1)
    @EventListener
    public void failing(BrokenEvent event) {
        throw new IllegalStateException("Słuchacz zepsuty: " + event.reason());
    }

    @Order(2)
    @EventListener
    public void afterFailing(BrokenEvent event) {
        log.add("drugi-słuchacz-uszkodzonego");
    }
}
