package s20_modulith.mod03_documentation.products;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * API modułu {@code products}: ceny produktów (w PLN). Moduł bazowy — nie zależy od żadnego innego.
 */
@Service
public class ProductCatalog {

    private final Map<String, BigDecimal> prices = Map.of(
            "ELE-001", new BigDecimal("5499.99"),
            "KSI-001", new BigDecimal("79.00"),
            "SPO-001", new BigDecimal("64.99"));

    /** Cena produktu albo pusty Optional, gdy katalog go nie zna. */
    public Optional<BigDecimal> priceOf(String sku) {
        return Optional.ofNullable(prices.get(sku));
    }
}
