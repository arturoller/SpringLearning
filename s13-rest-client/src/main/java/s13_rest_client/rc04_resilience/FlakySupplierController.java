package s13_rest_client.rc04_resilience;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FlakySupplierController = UDAWANY, „kapryśny” dostawca (flaky = zawodny, kapryśny) dla lekcji Rc04.
 * Zachowuje się przewidywalnie (liczniki, bez losowania), żeby plik .http dawał zawsze te same wyniki:
 * <ul>
 *   <li>GET /prices/{sku} — 1. i 2. zapytanie: 503, 3. zapytanie: 200 z ceną, i tak w kółko (awaria przejściowa),</li>
 *   <li>POST /orders — ZAPISUJE zamówienie, ale co drugie zapytanie (1., 3., 5. ...) kończy się 503, jakby odpowiedź
 *       zginęła po drodze. Z nagłówkiem Idempotency-Key ponowione zapytanie NIE tworzy drugiego zamówienia,</li>
 *   <li>GET /orders — lista zapisanych zamówień (tu widać duplikaty),</li>
 *   <li>GET /report — raport magazynowy.</li>
 * </ul>
 * Metody są synchronized: serwer obsługuje zapytania w wielu wątkach naraz (t21_concurrency/Concurrency02RaceConditions).
 */
@RestController
@RequestMapping("/supplier/api")
public class FlakySupplierController {

    private final Map<String, BigDecimal> prices = Map.of(
            "ELE-001", new BigDecimal("5499.99"), "SPO-001", new BigDecimal("64.99"), "KSI-001", new BigDecimal("79.00"));
    private final List<SupplierOrderConfirmation> orders = new ArrayList<>();
    private final Map<String, SupplierOrderConfirmation> ordersByKey = new HashMap<>();
    private int priceCalls;
    private int orderCalls;
    private long nextOrderNumber = 1001;

    @GetMapping("/prices/{sku}")
    public synchronized ResponseEntity<SupplierPrice> price(@PathVariable String sku) {
        priceCalls++;
        if (priceCalls % 3 != 0) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();   // awaria przejściowa
        }
        BigDecimal price = prices.get(sku);
        return price == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(new SupplierPrice(sku, price));
    }

    @PostMapping("/orders")
    public synchronized ResponseEntity<SupplierOrderConfirmation> order(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody SupplierOrder order) {
        SupplierOrderConfirmation confirmation = idempotencyKey == null ? null : ordersByKey.get(idempotencyKey);
        if (confirmation == null) {                                    // nowe zamówienie — zapisujemy
            confirmation = new SupplierOrderConfirmation(
                    "DOST-" + nextOrderNumber++, order.sku(), order.quantity(), "PRZYJETE");
            orders.add(confirmation);
            if (idempotencyKey != null) {
                ordersByKey.put(idempotencyKey, confirmation);
            }
        }
        orderCalls++;
        if (orderCalls % 2 == 1) {
            // Zamówienie JEST zapisane, ale klient dostaje błąd — tak wygląda zgubiona odpowiedź z jego strony.
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.created(URI.create("/supplier/api/orders/" + confirmation.orderId())).body(confirmation);
    }

    @GetMapping("/orders")
    public synchronized List<SupplierOrderConfirmation> orders() {
        return List.copyOf(orders);
    }

    @GetMapping("/report")
    public StockReport report() {
        return new StockReport(prices.size(), 142);
    }
}
