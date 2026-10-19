package s20_modulith.mod02_module_events.inventory.internal;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import s20_modulith.mod02_module_events.inventory.StockReserved;
import s20_modulith.mod02_module_events.orders.OrderPlaced;

/**
 * Szczegół wewnętrzny modułu {@code inventory}: słuchacz zdarzeń z innych modułów (event handler = obsługa zdarzeń).
 * Reaguje na {@link OrderPlaced}: rezerwuje towar (zmniejsza stan) i publikuje {@link StockReserved}.
 */
@Component
public class InventoryEventHandler {

    private final JdbcClient jdbc;
    private final ApplicationEventPublisher events;

    public InventoryEventHandler(JdbcClient jdbc, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    /**
     * {@code @ApplicationModuleListener} (słuchacz modułu aplikacji) = {@code @Async} + {@code @Transactional
     * (propagation = REQUIRES_NEW)} + {@code @TransactionalEventListener}: metoda biegnie w innym wątku, dopiero
     * po zatwierdzeniu transakcji, która opublikowała zdarzenie, i we WŁASNEJ nowej transakcji.
     *
     * @throws IllegalStateException gdy magazyn nie zna produktu albo ma za mało sztuk — publikacja zostaje wtedy
     *                               w rejestrze jako nieukończona i można ją ponowić
     */
    @ApplicationModuleListener
    public void on(OrderPlaced event) {
        int updated = jdbc.sql("UPDATE stock SET quantity = quantity - :quantity WHERE sku = :sku AND quantity >= :quantity")
                .param("quantity", event.quantity())
                .param("sku", event.sku())
                .update();
        if (updated == 0) {
            throw new IllegalStateException("Nie można zarezerwować %d szt. %s dla zamówienia %s"
                    .formatted(event.quantity(), event.sku(), event.orderId()));
        }
        events.publishEvent(new StockReserved(event.orderId(), event.sku(), event.quantity()));
    }
}
