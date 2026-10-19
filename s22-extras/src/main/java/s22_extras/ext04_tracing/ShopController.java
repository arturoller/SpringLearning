package s22_extras.ext04_tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s22_extras.ext04_tracing.AvailabilityService.Availability;

/** Punkt wejścia lekcji Ext04Tracing: GET /api/shop/{sku}/availability (do sprawdzania ręcznego i w teście). */
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final AvailabilityService availabilityService;
    private final Tracer tracer;

    public ShopController(AvailabilityService availabilityService, Tracer tracer) {
        this.availabilityService = availabilityService;
        this.tracer = tracer;   // Tracer (Micrometer Tracing) — fasada na OpenTelemetry, jak SLF4J na Logback
    }

    @GetMapping("/{sku}/availability")
    public Availability availability(@PathVariable String sku) {
        Span current = tracer.currentSpan();   // odcinek serwera HTTP utworzony przez obserwację Spring MVC
        String traceId = current == null ? "brak" : current.context().traceId();
        return availabilityService.check(sku, traceId);
    }
}
