package s18_aop.aop03_custom_annotations;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Zamówienia. placeOrder ma DWIE adnotacje ({@code @AuditLog} i {@code @MeasureTime}) — o kolejności aspektów
 * decyduje {@code @Order} na klasach aspektów (sekcja 3). placeOrderSafely ma zły {@code @SafeCall} (sekcja 4).
 */
@Service
public class OrderService {

    private static final Map<String, Integer> STOCK = Map.of("SPO-001", 120, "KSI-001", 15, "ELE-002", 0);

    private final AuditJournal journal;

    public OrderService(AuditJournal journal) {
        this.journal = journal;
    }

    @AuditLog(action = "NOWE_ZAMOWIENIE")
    @MeasureTime(warnAboveMillis = 100)
    public String placeOrder(String sku, int quantity) {
        journal.add("METODA placeOrder(" + sku + ", " + quantity + ")");
        return accept(sku, quantity);
    }

    @SafeCall
    public String placeOrderSafely(String sku, int quantity) {
        journal.add("METODA placeOrderSafely(" + sku + ", " + quantity + ")");
        return accept(sku, quantity);
    }

    private String accept(String sku, int quantity) {
        int stock = STOCK.getOrDefault(sku, 0);
        if (stock < quantity) {
            throw new IllegalStateException("Brak towaru: " + sku + " (stan " + stock + ", zamówiono " + quantity + ")");
        }
        return "Zamówienie przyjęte: " + quantity + " x " + sku;
    }
}
