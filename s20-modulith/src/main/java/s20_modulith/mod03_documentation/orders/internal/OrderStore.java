package s20_modulith.mod03_documentation.orders.internal;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Szczegół wewnętrzny modułu {@code orders}: lista numerów złożonych zamówień w pamięci (store = magazyn danych).
 * Na kanwie modułu (Documenter) domyślnie go nie widać — kanwa pokazuje API, a środek dopiero z
 * {@code CanvasOptions.revealInternals()}.
 */
@Component
public class OrderStore {

    private final List<String> orderIds = new CopyOnWriteArrayList<>();

    public void add(String orderId) {
        orderIds.add(orderId);
    }

    public List<String> all() {
        return List.copyOf(orderIds);
    }
}
