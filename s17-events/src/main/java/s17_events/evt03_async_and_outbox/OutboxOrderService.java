package s17_events.evt03_async_and_outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Serwis zamówień z wzorcem OUTBOX: zamówienie i wiadomość do wysłania zapisują się w TEJ SAMEJ transakcji bazy,
 * więc albo są oba, albo żadne. Wysyłką zajmuje się osobno {@link OutboxRelay}.
 */
@Service
public class OutboxOrderService {

    private final OrderRepository repository;

    public OutboxOrderService(OrderRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void placeOrder(String orderId, String customer, BigDecimal totalPln) {
        repository.insertOrder(orderId, customer, totalPln);
        repository.insertOutbox(orderId, "OrderPlaced", "orderId=" + orderId + ";customer=" + customer);
    }

    /** To samo, ale po zapisie rzuca wyjątek: wycofanie obejmuje i zamówienie, i wiadomość. */
    @Transactional
    public void placeOrderAndFail(String orderId, String customer, BigDecimal totalPln) {
        placeOrder(orderId, customer, totalPln);
        throw new IllegalStateException("Płatność odrzucona");
    }
}
