package s15_actuator_logging.obs04_logging;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler składania zamówień — zapytanie przechodzi najpierw przez RequestIdFilter, który ustawia MDC. */
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping
    public String checkout(@RequestBody NewCheckout checkout) {
        return service.checkout(checkout);
    }
}
