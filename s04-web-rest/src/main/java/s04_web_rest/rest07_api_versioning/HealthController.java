package s04_web_rest.rest07_api_versioning;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * HealthController = stan aplikacji — metoda BEZ atrybutu version (sekcja 4 lekcji Rest07ApiVersioning).
 * Pokazuje, jak wersjonowanie traktuje punkty końcowe, które wersji nie deklarują.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
