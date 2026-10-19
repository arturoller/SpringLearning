package s19_deploy.dep04_startup_and_shutdown;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler z „wolnym” punktem końcowym — do pokazania, co się dzieje z żądaniem w toku podczas zamykania. */
@RestController
public class SlowOrderController {

    private final OrderGate gate;

    public SlowOrderController(OrderGate gate) {
        this.gate = gate;
    }

    @GetMapping("/api/slow-order")
    public String slowOrder() throws InterruptedException {
        boolean released = gate.enterAndWait();
        return released ? "ZAM-001 gotowe" : "przerwane";
    }

    @GetMapping("/api/ping")
    public String ping() {
        return "pong";
    }
}
