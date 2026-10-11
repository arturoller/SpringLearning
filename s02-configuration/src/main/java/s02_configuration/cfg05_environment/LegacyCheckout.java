package s02_configuration.cfg05_environment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stara kasa — działa, gdy flaga nowej kasy jest wyłączona ({@code false}) ALBO w ogóle nie ma jej w konfiguracji
 * ({@code matchIfMissing = true}). Dzięki temu w KAŻDEJ konfiguracji istnieje dokładnie jedna kasa.
 */
@Component
@ConditionalOnProperty(prefix = "cfg05.feature", name = "new-checkout", havingValue = "false", matchIfMissing = true)
public class LegacyCheckout implements Checkout {

    @Override
    public String version() {
        return "stara";
    }
}
