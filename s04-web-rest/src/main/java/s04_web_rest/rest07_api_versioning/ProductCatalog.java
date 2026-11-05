package s04_web_rest.rest07_api_versioning;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Wspólne dane dla wszystkich wersji API. Wersje różnią się KSZTAŁTEM odpowiedzi (DTO), a nie danymi — dlatego
 * dane są w jednym miejscu, a kontroler tylko „tłumaczy” je na ProductV1 albo ProductV2.
 */
@Component
public class ProductCatalog {

    /** Produkt „w środku” aplikacji: cena w groszach, stan magazynu. */
    public record Item(long id, String name, long priceGrosze, int stock) {
    }

    private final Map<Long, Item> items = Map.of(
            1L, new Item(1, "Kawa", 2490, 12),
            2L, new Item(2, "Herbata", 1590, 0));

    public Optional<Item> find(long id) {
        return Optional.ofNullable(items.get(id));
    }

    public ProductV1 toV1(Item item) {
        return new ProductV1(item.id(), item.name(), item.priceGrosze());
    }

    public ProductV2 toV2(Item item) {
        BigDecimal amount = BigDecimal.valueOf(item.priceGrosze(), 2);   // 2490 groszy → 24.90
        return new ProductV2(item.id(), item.name(), new ProductV2.Price(amount, "PLN"));
    }
}
