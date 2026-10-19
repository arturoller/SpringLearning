package s15_actuator_logging.obs02_health_indicators;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Własny wskaźnik zdrowia (health indicator). Nazwa beana to "warehouseHealthIndicator"; w odpowiedzi
 * /actuator/health pojawia się pod nazwą "warehouse" — Boot ucina końcówkę "HealthIndicator".
 * W Boot 4 interfejs jest w pakiecie org.springframework.boot.health.contributor (w Boot 3: ...actuate.health).
 */
@Component
public class WarehouseHealthIndicator implements HealthIndicator {

    private final Warehouse warehouse;

    public WarehouseHealthIndicator(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    @Override   // override = nadpisanie metody z interfejsu
    public Health health() {
        if (warehouse.isAvailable()) {
            return Health.up().withDetail("items", warehouse.itemCount()).build();   // up = działa; detail = szczegół
        }
        return Health.down().withDetail("reason", "magazyn nie odpowiada").build();   // down = nie działa
    }
}
