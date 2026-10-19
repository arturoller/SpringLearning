package s18_aop.aop02_proxies;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Klasa Z INTERFEJSEM (PriceCalculator) i z jedną metodą SPOZA interfejsu: currency().
 * W Boot (proxy CGLIB) bean jest instancją tej klasy; przy proxy JDK — tylko instancją interfejsu (sekcja 3).
 */
@Service
public class StandardPriceCalculator implements PriceCalculator {

    private final Map<String, BigDecimal> prices = Map.of(
            "SPO-001", new BigDecimal("64.99"),
            "KSI-001", new BigDecimal("79.00"),
            "ELE-003", new BigDecimal("349.90"));

    @Override
    public BigDecimal priceFor(String sku, int quantity) {
        BigDecimal price = prices.get(sku);
        if (price == null) {
            throw new IllegalArgumentException("Nieznany SKU: " + sku);
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /** Metoda, której NIE ma w interfejsie — przez proxy JDK nie da się jej wywołać (proxy JDK „zna” tylko interfejs). */
    public String currency() {
        return "PLN";
    }
}
