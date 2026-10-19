package s12_security.sec04_stateless_api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * ApiController = API sklepu dla programów (aplikacja mobilna, frontend w JavaScript, inny serwer).
 * Obsługuje je łańcuch apiChain: bez sesji, bez CSRF, z CORS i błędami 401/403 w JSON.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    /** Nowe zamówienie: numer produktu i liczba sztuk. */
    public record NewOrder(String sku, int quantity) {
    }

    @GetMapping("/products")
    public List<String> products() {
        return List.of("ELE-003 Słuchawki BT", "DOM-001 Ekspres do kawy");
    }

    @GetMapping("/orders")
    public Map<String, Object> orders(Principal principal) {
        return Map.of("user", principal.getName(), "orders", List.of("ZAM-002", "ZAM-003"));
    }

    /** POST bez tokenu CSRF — w tym łańcuchu CSRF jest wyłączone. */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> create(@RequestBody NewOrder order, Principal principal) {
        Map<String, Object> created = Map.of("id", "ZAM-004", "user", principal.getName(),
                "sku", order.sku(), "quantity", order.quantity());
        return ResponseEntity.created(URI.create("/api/orders/ZAM-004")).body(created);
    }

    @GetMapping("/admin/report")
    public Map<String, Object> report() {
        return Map.of("report", "Sprzedaż dzienna", "ordersToday", 12);
    }
}
