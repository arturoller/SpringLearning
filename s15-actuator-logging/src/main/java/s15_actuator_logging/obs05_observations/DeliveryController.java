package s15_actuator_logging.obs05_observations;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler wysyłek: POST /api/deliveries?orderId=ZAM-001&carrier=DHL. */
@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService service;

    public DeliveryController(DeliveryService service) {
        this.service = service;
    }

    @PostMapping
    public String ship(@RequestParam String orderId, @RequestParam String carrier) {   // request param = parametr zapytania
        return service.ship(orderId, carrier);
    }
}
