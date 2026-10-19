package s15_actuator_logging.obs03_metrics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler zamówień — to jego zapytania Boot mierzy sam jako metrykę http.server.requests. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping   // post mapping = mapowanie metody POST
    public String place(@RequestBody NewOrder order) {   // request body = ciało zapytania (JSON → rekord)
        return service.placeOrder(order);
    }

    @PostMapping("/ship")
    public String ship() {
        service.shipOrder();
        return "wysłano";
    }

    @GetMapping("/{id}")   // path variable w adresie: tu odpowiadamy tym samym tekstem
    public String one(@PathVariable String id) {
        return id;
    }
}
