package s18_aop.aop03_custom_annotations;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cennik. changePrice ma {@code @AuditLog} (działa), previewPrice — zepsutą adnotację BrokenAuditLog (nie działa).
 * Ceny w złotych jako BigDecimal; mapa współbieżna, bo serwis jest singletonem używanym przez wiele wątków.
 */
@Service
public class PriceService {

    private final Map<String, BigDecimal> prices = new ConcurrentHashMap<>(Map.of(
            "SPO-001", new BigDecimal("64.99"),
            "KSI-001", new BigDecimal("79.00"),
            "DOM-002", new BigDecimal("129.00")));

    private final AuditJournal journal;

    public PriceService(AuditJournal journal) {
        this.journal = journal;
    }

    @AuditLog(action = "ZMIANA_CENY")
    public BigDecimal changePrice(String sku, BigDecimal newPrice) {
        journal.add("METODA changePrice(" + sku + ", " + newPrice + ")");
        if (newPrice.signum() <= 0) {
            throw new IllegalArgumentException("Cena musi być dodatnia: " + newPrice);
        }
        prices.put(sku, newPrice);
        return newPrice;
    }

    @BrokenAuditLog("PODGLĄD_CENY")
    public BigDecimal previewPrice(String sku) {
        journal.add("METODA previewPrice(" + sku + ")");
        return prices.get(sku);
    }
}
