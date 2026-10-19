package s20_modulith.mod03_documentation.orders;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s20_modulith.mod03_documentation.orders.events.OrderPlaced;
import s20_modulith.mod03_documentation.orders.internal.OrderStore;
import s20_modulith.mod03_documentation.products.ProductCatalog;

import java.math.BigDecimal;

/**
 * API modułu {@code orders}: składanie zamówień. Używa beana z modułu {@code products} (na diagramie: strzałka
 * „uses”) i publikuje zdarzenie {@link OrderPlaced} (moduł {@code inventory} go słucha: strzałka „listens to”).
 */
@Service
public class OrderManagement {

    private final ProductCatalog catalog;
    private final OrderStore store;
    private final ApplicationEventPublisher events;

    public OrderManagement(ProductCatalog catalog, OrderStore store, ApplicationEventPublisher events) {
        this.catalog = catalog;
        this.store = store;
        this.events = events;
    }

    /** Składa zamówienie i publikuje OrderPlaced; nieznany produkt → IllegalArgumentException. */
    @Transactional
    public BigDecimal placeOrder(String orderId, String sku, int quantity) {
        BigDecimal price = catalog.priceOf(sku)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma produktu " + sku));
        BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));
        store.add(orderId);
        events.publishEvent(new OrderPlaced(orderId, sku, quantity, total));
        return total;
    }
}
