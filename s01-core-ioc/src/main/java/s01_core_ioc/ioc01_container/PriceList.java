package s01_core_ioc.ioc01_container;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PriceList = cennik. {@code @Component} (komponent) — Spring utworzy z tej klasy bean (ziarno — obiekt zarządzany
 * przez Springa) i poda go każdemu, kto go potrzebuje.
 */
@Component
public class PriceList {

    private static final Map<String, Integer> PRICES_GROSZE = Map.of(
            "Kawa", 2999,
            "Herbata", 1599,
            "Czekolada", 899);

    /** Cena produktu w groszach; nieznany produkt → IllegalArgumentException. */
    public int priceOf(String product) {
        Integer price = PRICES_GROSZE.get(product);
        if (price == null) {
            throw new IllegalArgumentException("Nieznany produkt: " + product);
        }
        return price;
    }
}
