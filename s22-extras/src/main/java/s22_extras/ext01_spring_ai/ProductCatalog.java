package s22_extras.ext01_spring_ai;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/** Mały katalog produktów w pamięci (lekcja Ext01SpringAi) — wystarczy do pokazania ChatClienta. */
@Component
public class ProductCatalog {

    private final Map<String, CatalogProduct> products = Map.of(
            "ELE-003", new CatalogProduct("ELE-003", "Słuchawki BT", "ELEKTRONIKA", new BigDecimal("349.90")),
            "SPO-001", new CatalogProduct("SPO-001", "Kawa ziarnista 1kg", "SPOZYWCZE", new BigDecimal("64.99")),
            "KSI-001", new CatalogProduct("KSI-001", "Czysty kod", "KSIAZKI", new BigDecimal("79.00")));

    public Optional<CatalogProduct> find(String sku) {
        return Optional.ofNullable(products.get(sku));
    }
}
