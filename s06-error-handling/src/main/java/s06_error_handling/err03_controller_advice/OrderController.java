package s06_error_handling.err03_controller_advice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler objęty "wąskim" advice (OrderAdvice ma {@code assignableTypes = OrderController.class}). */
@RestController
public class OrderController {

    @GetMapping("/api/orders/conflict")
    public String conflict() {
        throw new IllegalStateException("Zamówienie jest już zrealizowane");
    }

    @GetMapping("/api/orders/product")
    public String product() {
        throw new ProductNotFoundException(999);
    }

    @GetMapping("/api/orders/pay")
    public String pay() {
        throw new PaymentDeclinedException("Bank odrzucił kartę");
    }
}
