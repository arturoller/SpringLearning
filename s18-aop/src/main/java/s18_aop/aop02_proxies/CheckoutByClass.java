package s18_aop.aop02_proxies;

import java.math.BigDecimal;

/**
 * Kasa, która chce kalkulator przez KLASĘ (StandardPriceCalculator), a nie przez interfejs (PriceCalculator).
 * Przy proxy CGLIB to działa (proxy jest podklasą), przy proxy JDK aplikacja nie startuje (sekcja 3).
 * Bez {@code @Component} — rejestruje ją tylko test (inaczej popsułaby kontekst lekcji w trybie JDK).
 */
public class CheckoutByClass {

    private final StandardPriceCalculator calculator;

    public CheckoutByClass(StandardPriceCalculator calculator) {
        this.calculator = calculator;
    }

    public BigDecimal total(String sku, int quantity) {
        return calculator.priceFor(sku, quantity);
    }
}
