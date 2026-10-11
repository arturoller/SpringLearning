package s04_web_rest.rest06_cors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * OrderCorsController = zamówienia dla panelu administracyjnego — sekcja 4 lekcji Rest06Cors.
 * BEZ adnotacji CORS: zasady dla /api/orders/** ustawia globalnie CorsConfig (panel: GET i POST, bez DELETE).
 */
@RestController
@RequestMapping("/api/orders")
public class OrderCorsController {

    @GetMapping
    public List<String> orders() {
        return List.of("zamówienie-1", "zamówienie-2");
    }

    @PostMapping
    public Map<String, String> create(@RequestBody Map<String, String> order) {
        return Map.of("status", "przyjęte", "customer", order.getOrDefault("customer", "?"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return ResponseEntity.noContent().build();
    }
}
