package s13_rest_client.rc02_errors_and_timeouts;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * FakeSupplierController = UDAWANY serwer dostawcy dla lekcji Rc02 — umie też się psuć.
 * Tak jak środowiska testowe (sandbox — piaskownica) prawdziwych API, ma SPECJALNE kody produktów, które wywołują awarie:
 * <ul>
 *   <li>{@code AWARIA} → 500 Internal Server Error (błąd w aplikacji dostawcy),</li>
 *   <li>{@code PRZERWA} → 503 Service Unavailable + nagłówek Retry-After: 120 (przerwa techniczna),</li>
 *   <li>{@code WOLNY} → odpowiedź dopiero po podniesieniu szlabanu SlowSupplierGate (najdłużej 5 s) — do limitów czasu,</li>
 *   <li>nieznany kod → 404 Not Found z ciałem ProblemDetail (RFC 9457 — s06_error_handling/Err04ProblemDetail).</li>
 * </ul>
 */
@RestController
@RequestMapping("/supplier/api")
public class FakeSupplierController {

    private static final Duration MAX_WAIT = Duration.ofSeconds(5);

    private final Map<String, Integer> stock = Map.of("ELE-001", 7, "ELE-002", 0, "SPO-001", 120, "KSI-001", 15);
    private final SlowSupplierGate gate;

    public FakeSupplierController(SlowSupplierGate gate) {
        this.gate = gate;
    }

    @GetMapping("/stock/{sku}")
    public ResponseEntity<?> stock(@PathVariable String sku) throws InterruptedException {
        switch (sku) {
            case "AWARIA" -> {
                return ResponseEntity.of(ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Błąd bazy danych dostawcy")).build();
            }
            case "PRZERWA" -> {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After", "120").build();
            }
            case "WOLNY" -> {
                gate.awaitOpen(MAX_WAIT);
                return ResponseEntity.ok(new StockLevel(sku, 1));
            }
            default -> {
                Integer available = stock.get(sku);
                if (available == null) {
                    return ResponseEntity.of(ProblemDetail.forStatusAndDetail(
                            HttpStatus.NOT_FOUND, "Nieznany produkt: " + sku)).build();
                }
                return ResponseEntity.ok(new StockLevel(sku, available));
            }
        }
    }
}
