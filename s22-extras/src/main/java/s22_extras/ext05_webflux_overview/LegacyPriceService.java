package s22_extras.ext05_webflux_overview;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * „Stary” serwis z BLOKUJĄCYM API (lekcja Ext05WebFluxOverview, sekcja 4) — w prawdziwym życiu: JDBC, JPA, stary
 * klient SOAP, odczyt pliku. Wywołanie zajmuje wątek, aż przyjdzie wynik.
 */
@Service
public class LegacyPriceService {

    /** Cena hurtowa: 80% ceny katalogowej (udajemy zapytanie do bazy). */
    public BigDecimal wholesalePrice(ReactiveProduct product) {
        return product.price().multiply(new BigDecimal("0.80")).setScale(2, RoundingMode.HALF_UP);
    }
}
