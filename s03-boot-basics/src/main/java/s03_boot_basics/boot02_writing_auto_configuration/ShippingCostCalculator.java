package s03_boot_basics.boot02_writing_auto_configuration;

import java.math.BigDecimal;

/**
 * Klasa pomocnicza ĆWICZEŃ 2 i 3: koszt wysyłki. Zamówienie za co najmniej {@code freeFrom} — wysyłka darmowa,
 * taniej — 15 zł. Zwykła klasa bez adnotacji; beanem robi ją Twoja autokonfiguracja.
 */
public class ShippingCostCalculator {

    private static final BigDecimal STANDARD_COST = new BigDecimal("15");

    private final BigDecimal freeFrom;

    public ShippingCostCalculator(BigDecimal freeFrom) {
        this.freeFrom = freeFrom;
    }

    public BigDecimal cost(BigDecimal orderTotal) {
        return orderTotal.compareTo(freeFrom) >= 0 ? BigDecimal.ZERO : STANDARD_COST;
    }

    public BigDecimal freeFrom() {
        return freeFrom;
    }
}
