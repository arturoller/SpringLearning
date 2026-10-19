package s22_extras.ext04_tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * „Usługa magazynu” (lekcja Ext04Tracing). W prawdziwym systemie to OSOBNA aplikacja — tu, dla prostoty, ten sam
 * proces, ale wołany przez HTTP (RestClient), więc ślad przechodzi przez sieć dokładnie tak, jak między usługami.
 */
@RestController
@RequestMapping("/api/stock")
public class StockController {

    /** Stan magazynu i nagłówek traceparent, który przyszedł z zapytaniem (do pokazania propagacji). */
    public record StockLevel(String sku, int stock, String receivedTraceparent) {
    }

    private static final Logger log = LoggerFactory.getLogger(StockController.class);

    private static final Map<String, Integer> STOCK = Map.of("ELE-001", 7, "ELE-002", 0, "SPO-001", 120);

    @GetMapping("/{sku}")
    public StockLevel stock(@PathVariable String sku,
                            @RequestHeader(name = "traceparent", required = false) String traceparent) {
        // W logu przed komunikatem pojawi się [traceId-spanId] — Boot wstawia je z MDC (logging.pattern.correlation)
        log.info("Magazyn: sprawdzam stan {}", sku);
        return new StockLevel(sku, STOCK.getOrDefault(sku, 0), traceparent);
    }
}
