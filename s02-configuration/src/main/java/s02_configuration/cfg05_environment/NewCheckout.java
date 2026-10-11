package s02_configuration.cfg05_environment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Nowa kasa — powstaje TYLKO gdy {@code cfg05.feature.new-checkout=true}.
 * ({@code @ConditionalOnProperty} = warunkowo, zależnie od właściwości; havingValue = mająca wartość;
 * matchIfMissing = pasuje, gdy klucza brak.) Brak klucza → ten bean NIE powstaje (domyślne matchIfMissing=false).
 */
@Component
@ConditionalOnProperty(prefix = "cfg05.feature", name = "new-checkout", havingValue = "true")
public class NewCheckout implements Checkout {

    @Override
    public String version() {
        return "nowa";
    }
}
