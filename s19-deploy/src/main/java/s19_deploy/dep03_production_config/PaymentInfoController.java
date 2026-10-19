package s19_deploy.dep03_production_config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Pokazuje, z jakimi ustawieniami działa aplikacja — klucz API zawsze zamaskowany. */
@RestController
public class PaymentInfoController {

    private final PaymentSettings settings;

    public PaymentInfoController(PaymentSettings settings) {
        this.settings = settings;
    }

    @GetMapping("/api/payment-settings")
    public Map<String, String> settings() {
        return Map.of(
                "url", settings.url().toString(),
                "apiKey", settings.maskedApiKey(),
                "timeout", settings.timeout().toString());
    }
}
