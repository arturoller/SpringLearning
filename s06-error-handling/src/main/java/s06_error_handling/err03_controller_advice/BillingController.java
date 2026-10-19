package s06_error_handling.err03_controller_advice;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler rozliczeń — objęty wąską poradą BillingAdvice (przez {@code assignableTypes}). */
@RestController
public class BillingController {

    @PostMapping("/api/billing/charge")
    public String charge() {
        throw new IllegalStateException("Bramka płatności nie odpowiada (timeout 5000 ms)");
    }
}
