package s11_testing.test02_mockito_beans;

import java.math.BigDecimal;

/**
 * Zewnętrzna usługa cen (np. hurtownia podająca aktualne ceny przez HTTP). To INTERFEJS — dzięki temu test może
 * podstawić atrapę (@MockitoBean) albo własną prostą implementację (fake) zamiast prawdziwego HTTP.
 */
public interface PriceClient {

    /** @throws PriceUnavailableException gdy usługa nie odpowiada */
    BigDecimal currentPrice(String productName);
}
