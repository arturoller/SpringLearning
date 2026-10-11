package s02_configuration.cfg03_profiles;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Bramka „na niby” (fake = sztuczny, atrapa): nigdzie nie łączy się z siecią, zawsze przyjmuje płatność.
 * {@code @Profile("!cfg03-prod")} — wykrzyknik to negacja: bean istnieje, gdy profil cfg03-prod NIE jest aktywny
 * (czyli w rozwoju, w testach i gdy nie ma żadnego profilu). Dzięki temu nikt przypadkiem nie obciąży prawdziwej karty.
 */
@Component
@Profile("!cfg03-prod")
public class FakePaymentGateway implements PaymentGateway {

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public String charge(BigDecimal amount) {
        return "FAKE: przyjęto " + amount;
    }
}
