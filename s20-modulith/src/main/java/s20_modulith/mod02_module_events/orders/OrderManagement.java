package s20_modulith.mod02_module_events.orders;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * API modułu {@code orders}: składanie zamówień (management = zarządzanie). Zapisuje zamówienie i PUBLIKUJE
 * zdarzenie {@link OrderPlaced} — nie wie, kto go słucha. Moduł {@code orders} nie zależy od modułu magazynu:
 * to magazyn zależy od zamówień (zna typ {@code OrderPlaced}).
 */
@Service
public class OrderManagement {

    private final JdbcClient jdbc;
    private final ApplicationEventPublisher events;

    public OrderManagement(JdbcClient jdbc, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    /**
     * Składa zamówienie w JEDNEJ transakcji: wiersz w tabeli orders + zdarzenie OrderPlaced. Rejestr publikacji
     * Spring Modulith zapisuje zdarzenie w tabeli EVENT_PUBLICATION w tej samej transakcji, a słuchacze
     * {@code @ApplicationModuleListener} dostaną je dopiero po jej zatwierdzeniu.
     */
    @Transactional
    public void placeOrder(String orderId, String sku, int quantity) {
        jdbc.sql("INSERT INTO orders (order_id, sku, quantity, status) VALUES (:id, :sku, :quantity, 'NOWE')")
                .param("id", orderId)
                .param("sku", sku)
                .param("quantity", quantity)
                .update();
        events.publishEvent(new OrderPlaced(orderId, sku, quantity));
    }

    /** Status zamówienia albo pusty Optional, gdy zamówienia nie ma. */
    public Optional<String> statusOf(String orderId) {
        return jdbc.sql("SELECT status FROM orders WHERE order_id = :id")
                .param("id", orderId)
                .query(String.class)
                .optional();
    }
}
