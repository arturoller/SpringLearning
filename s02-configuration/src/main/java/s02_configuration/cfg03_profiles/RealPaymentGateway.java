package s02_configuration.cfg03_profiles;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * „Prawdziwa” bramka — istnieje tylko w profilu produkcyjnym {@code cfg03-prod}. W lekcji nie łączy się z siecią
 * (to byłoby niedeterministyczne w testach); pokazuje tylko, że dostała adres z pliku profilu.
 */
@Component
@Profile("cfg03-prod")
public class RealPaymentGateway implements PaymentGateway {

    private final GatewaySettings settings;

    public RealPaymentGateway(GatewaySettings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "real";
    }

    @Override
    public String charge(BigDecimal amount) {
        return "REAL: obciążono " + amount + " przez " + settings.url();
    }
}
