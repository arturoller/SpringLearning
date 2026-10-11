package s04_web_rest.rest06_cors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * InternalStatsController = wewnętrzne statystyki — sekcje 2 i 5 lekcji Rest06Cors. ŻADNEJ konfiguracji CORS:
 * przeglądarka z obcej strony nie przeczyta odpowiedzi. Ale curl, Postman czy inny serwer — tak (CORS ich nie dotyczy).
 */
@RestController
public class InternalStatsController {

    @GetMapping("/api/internal/stats")
    public Map<String, Integer> stats() {
        return Map.of("ordersToday", 42);
    }
}
