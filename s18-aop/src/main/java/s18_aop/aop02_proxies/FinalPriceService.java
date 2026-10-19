package s18_aop.aop02_proxies;

import java.math.BigDecimal;

/**
 * Klasa FINAL z interfejsem, BEZ {@code @Proxyable} i BEZ {@code @Component} — rejestruje ją tylko test sekcji 5.
 * Nazwa beana kończy się na „Service”, więc pasuje do punktu przecięcia CountingAspect i Spring musi zrobić proxy:
 * CGLIB (domyślnie w Boot) nie umie dziedziczyć po klasie final → start się nie udaje; proxy JDK — działa.
 */
public final class FinalPriceService implements PriceCalculator {

    @Override
    public BigDecimal priceFor(String sku, int quantity) {
        return new BigDecimal("7.49").multiply(BigDecimal.valueOf(quantity));
    }
}
