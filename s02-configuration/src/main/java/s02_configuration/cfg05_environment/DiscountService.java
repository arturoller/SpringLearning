package s02_configuration.cfg05_environment;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Usługa rabatowa czytająca ustawienia WPROST z {@link Environment} (środowisko = zbiór źródeł ustawień i profili).
 * To „niskopoziomowy” sposób — w zwykłym kodzie wystarczą {@code @Value} i {@code @ConfigurationProperties};
 * Environment przydaje się, gdy nazwa klucza lub profil wyliczane są dopiero w czasie działania.
 */
@Service
public class DiscountService {

    private final Environment environment;

    public DiscountService(Environment environment) {
        this.environment = environment;
    }

    /** Rabat w procentach — klucz OBOWIĄZKOWY: brak = wyjątek IllegalStateException. */
    public int percent() {
        return environment.getRequiredProperty("cfg05.shop.discount-percent", Integer.class);
    }

    /** Etykieta — klucz opcjonalny z wartością domyślną. */
    public String label() {
        return environment.getProperty("cfg05.shop.label", "Sklep");
    }

    /** Tryb sprzedaży zależny od profilu. */
    public String mode() {
        return environment.acceptsProfiles(Profiles.of("cfg05-weekend")) ? "WEEKEND" : "ZWYKŁY";
    }

    /** Cena po rabacie, zaokrąglona do groszy (w dół). */
    public BigDecimal apply(BigDecimal price) {
        BigDecimal factor = BigDecimal.valueOf(100 - percent()).movePointLeft(2);
        return price.multiply(factor).setScale(2, java.math.RoundingMode.DOWN);
    }
}
