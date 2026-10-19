package s18_aop.aop02_proxies;

import java.math.BigDecimal;

/** Kalkulator cen — INTERFEJS. Ma go StandardPriceCalculator, więc ta klasa może dostać proxy JDK (sekcja 2–3). */
public interface PriceCalculator {

    /** Cena za {@code quantity} sztuk produktu (w złotych). */
    BigDecimal priceFor(String sku, int quantity);
}
