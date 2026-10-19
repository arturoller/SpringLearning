package s20_modulith.mod03_documentation.inventory;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API modułu {@code inventory}: stan magazynu w pamięci (liczba sztuk na SKU).
 */
@Service
public class InventoryManagement {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of("ELE-001", 7, "KSI-001", 15, "SPO-001", 120));

    public int stockOf(String sku) {
        return stock.getOrDefault(sku, 0);
    }

    /** Zmniejsza stan o podaną liczbę sztuk (wołane przez słuchacza zdarzeń modułu). */
    public void decrease(String sku, int quantity) {
        stock.merge(sku, -quantity, Integer::sum);
    }
}
