package s20_modulith.mod01_modules.products.internal;

import org.springframework.stereotype.Component;
import s20_modulith.mod01_modules.products.ProductView;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Szczegół WEWNĘTRZNY modułu {@code products} (internal = wewnętrzny): magazyn produktów w pamięci (store = magazyn).
 * Klasa jest {@code public}, bo musi jej użyć {@code ProductCatalog} z innego pakietu — ale leży w podpakiecie
 * modułu, więc dla Spring Modulith NIE należy do API. Gdy inny moduł sięgnie tu bezpośrednio, {@code verify()}
 * zgłosi naruszenie.
 */
@Component
public class ProductStore {

    private final Map<String, ProductView> products = Map.of(
            "ELE-001", new ProductView("ELE-001", "Laptop Pro 14", new BigDecimal("5499.99"), 7),
            "ELE-002", new ProductView("ELE-002", "Smartfon X", new BigDecimal("2999.00"), 0),
            "KSI-001", new ProductView("KSI-001", "Czysty kod", new BigDecimal("79.00"), 15),
            "SPO-001", new ProductView("SPO-001", "Kawa ziarnista 1kg", new BigDecimal("64.99"), 120));

    public Optional<ProductView> find(String sku) {
        return Optional.ofNullable(products.get(sku));
    }
}
