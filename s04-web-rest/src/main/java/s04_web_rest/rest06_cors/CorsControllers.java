package s04_web_rest.rest06_cors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * Kontrolery lekcji Rest06Cors — cztery różne sytuacje CORS w jednym pliku (każda klasa to osobny bean).
 */
final class CorsControllers {

    private CorsControllers() {
    }

    /** /api/shop/** — zgoda z GLOBALNEJ konfiguracji (CorsConfig): tylko frontend sklepu z http://localhost:3000. */
    @RestController
    @RequestMapping("/api/shop/products")
    static class ShopController {

        @GetMapping
        ResponseEntity<List<String>> all() {
            List<String> skus = List.of("ELE-001", "KSI-001", "SPO-001");
            return ResponseEntity.ok().header("X-Total-Count", String.valueOf(skus.size())).body(skus);
        }

        @PostMapping
        ResponseEntity<String> create(@RequestBody String sku) {
            return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{sku}")
                    .buildAndExpand(sku).toUri()).body(sku);
        }
    }

    /**
     * /api/public/** — dane publiczne (godziny otwarcia). {@code @CrossOrigin(origins = "*")} na METODZIE: każda strona
     * może je czytać. Bez ciasteczek (allowCredentials domyślnie false), więc "*" jest tu bezpieczne.
     */
    @RestController
    @RequestMapping("/api/public")
    static class PublicController {

        @CrossOrigin(origins = "*")
        @GetMapping("/opening-hours")
        String openingHours() {
            return "pon-pt 9-18, sob 10-14";
        }
    }

    /**
     * /api/partners/** — partnerzy z dowolnej subdomeny partner.example, z ciasteczkami (allowCredentials = "true").
     * originPatterns (wzorce pochodzeń) zamiast origins, bo "*" razem z credentials jest zabronione.
     */
    @RestController
    @RequestMapping("/api/partners")
    @CrossOrigin(originPatterns = "https://*.partner.example", allowCredentials = "true")
    static class PartnerController {

        @GetMapping("/stock")
        String stock() {
            return "ELE-001: 7, KSI-001: 15";
        }
    }

    /**
     * /api/internal/** — BEZ żadnej konfiguracji CORS. Serwer i tak odpowiada (200 + dane), ale bez nagłówka
     * Access-Control-Allow-Origin — przeglądarka nie pokaże tej odpowiedzi skryptowi z innej strony.
     */
    @RestController
    @RequestMapping("/api/internal")
    static class InternalController {

        @GetMapping("/report")
        String report() {
            return "Raport wewnętrzny: marża 23%";
        }
    }
}
