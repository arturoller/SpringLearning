package s17_events.evt01_application_events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Serwis zamówień (service = serwis, warstwa logiki). Zna TYLKO publikatora zdarzeń — nie wie, ilu słuchaczy
 * nasłuchuje ani co robią (luźne powiązanie).
 */
@Service
public class OrderService {

    private final ApplicationEventPublisher publisher;
    private final EventLog log;

    public OrderService(ApplicationEventPublisher publisher, EventLog log) {
        this.publisher = publisher;
        this.log = log;
    }

    public void placeOrder(String orderId, String customer, BigDecimal totalPln) {
        log.add("zamówienie-przyjęte:" + orderId);
        publisher.publishEvent(new OrderPlaced(orderId, customer, totalPln));
        log.add("publikacja-zakończona:" + orderId);
    }

    public void publishBroken(String reason) {
        publisher.publishEvent(new BrokenEvent(reason));
        log.add("po-publikacji-uszkodzonego");
    }
}
