package s20_modulith.mod03_broken.named_interface.inventory;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import s20_modulith.mod03_broken.named_interface.orders.OrderManagement;
import s20_modulith.mod03_broken.named_interface.orders.events.OrderPlaced;

/**
 * BŁĄD (celowy): słuchanie OrderPlaced (interfejs events) jest dozwolone, ale wstrzyknięcie OrderManagement
 * (interfejs nienazwany modułu orders) — już nie.
 */
@Component
public class InventoryEventHandler {

    private final OrderManagement orders;

    public InventoryEventHandler(OrderManagement orders) {
        this.orders = orders;
    }

    @ApplicationModuleListener
    public void on(OrderPlaced event) {
        System.out.println("Zamówień: " + orders.countOrders() + ", nowe: " + event.orderId());
    }
}
