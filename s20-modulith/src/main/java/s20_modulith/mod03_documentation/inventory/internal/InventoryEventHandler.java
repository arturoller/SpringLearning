package s20_modulith.mod03_documentation.inventory.internal;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import s20_modulith.mod03_documentation.inventory.InventoryManagement;
import s20_modulith.mod03_documentation.orders.events.OrderPlaced;

/**
 * Słuchacz zdarzeń modułu {@code inventory}: po złożeniu zamówienia zmniejsza stan. Używa z modułu {@code orders}
 * WYŁĄCZNIE nazwanego interfejsu {@code events} (typ OrderPlaced).
 */
@Component
public class InventoryEventHandler {

    private final InventoryManagement inventory;

    public InventoryEventHandler(InventoryManagement inventory) {
        this.inventory = inventory;
    }

    @ApplicationModuleListener
    public void on(OrderPlaced event) {
        inventory.decrease(event.sku(), event.quantity());
    }
}
