package s15_actuator_logging.obs02_health_indicators;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Przyciski do zabawy w lekcji (tylko do ćwiczeń z plikiem .http — NIE rób tak w prawdziwej aplikacji bez
 * zabezpieczeń): psują magazyn i przełączają gotowość (readiness) aplikacji.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final Warehouse warehouse;
    private final ApplicationContext context;   // context = kontekst aplikacji (kontener Springa)

    public AdminController(Warehouse warehouse, ApplicationContext context) {
        this.warehouse = warehouse;
        this.context = context;
    }

    @PostMapping("/warehouse/down")
    public String warehouseDown() {
        warehouse.setAvailable(false);
        return "magazyn: niedostępny";
    }

    @PostMapping("/warehouse/up")
    public String warehouseUp() {
        warehouse.setAvailable(true);
        return "magazyn: dostępny";
    }

    @PostMapping("/readiness/refuse")
    public String refuseTraffic() {
        // publish = opublikuj; REFUSING_TRAFFIC = odmawiam ruchu (aplikacja żyje, ale nie chce nowych zapytań)
        AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
        return "gotowość: REFUSING_TRAFFIC";
    }

    @PostMapping("/readiness/accept")
    public String acceptTraffic() {
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        return "gotowość: ACCEPTING_TRAFFIC";
    }
}
