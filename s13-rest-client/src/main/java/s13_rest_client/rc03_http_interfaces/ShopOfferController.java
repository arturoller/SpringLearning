package s13_rest_client.rc03_http_interfaces;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ShopOfferController = punkt końcowy NASZEGO sklepu: GET /api/shop/cheapest?category=ELEKTRONIKA →
 * najtańsza dostępna oferta dostawcy (200) albo 404, gdy w kategorii nic nie jest dostępne.
 * Droga zapytania: nasz kontroler → CheapestOfferService → SupplierApi (proxy) → RestClient → dostawca.
 */
@RestController
public class ShopOfferController {

    private final CheapestOfferService service;

    public ShopOfferController(CheapestOfferService service) {
        this.service = service;
    }

    @GetMapping("/api/shop/cheapest")
    public ResponseEntity<SupplierOffer> cheapest(@RequestParam String category) {
        return ResponseEntity.of(service.cheapestAvailable(category));   // Optional.empty() → 404
    }
}
