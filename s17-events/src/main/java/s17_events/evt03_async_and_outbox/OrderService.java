package s17_events.evt03_async_and_outbox;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Serwis zamówień publikujący zdarzenia (wersja BEZ outboxa) — punkt odniesienia dla sekcji 2–4. */
@Service
public class OrderService {

    private final OrderRepository repository;
    private final ApplicationEventPublisher publisher;

    public OrderService(OrderRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /** Zapis + zdarzenie obsługiwane przez {@link DirectNotificationListener} po commicie. */
    @Transactional
    public void placeOrder(String orderId, String customer, BigDecimal totalPln) {
        repository.insertOrder(orderId, customer, totalPln);
        publisher.publishEvent(new OrderPlaced(orderId, customer, totalPln));
    }

    /** Zapis + zdarzenie obsługiwane asynchronicznie po commicie. */
    @Transactional
    public void placeOrderAsync(String orderId, String customer, BigDecimal totalPln) {
        repository.insertOrder(orderId, customer, totalPln);
        publisher.publishEvent(new OrderPlacedAsync(orderId, customer, totalPln));
    }

    public void publishGated(String name) {
        publisher.publishEvent(new GatedEvent(name));
    }

    public void publishFailingAsync(String reason) {
        publisher.publishEvent(new FailingAsyncEvent(reason));
    }
}
