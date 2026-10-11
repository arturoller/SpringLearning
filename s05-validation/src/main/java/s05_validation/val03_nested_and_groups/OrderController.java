package s05_validation.val03_nested_and_groups;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s05_validation.val03_nested_and_groups.Val03Types.MixedDto;
import s05_validation.val03_nested_and_groups.Val03Types.OnCreate;
import s05_validation.val03_nested_and_groups.Val03Types.OnUpdate;
import s05_validation.val03_nested_and_groups.Val03Types.OrderRequest;
import s05_validation.val03_nested_and_groups.Val03Types.ProductDto;

import java.util.Map;

/** OrderController = kontroler zamówień i produktów do lekcji Val03NestedAndGroups (sekcje 1, 5). */
@RestController
@RequestMapping("/api")
public class OrderController {

    /** {@code @Valid} → grupa Default; zagnieżdżone obiekty sprawdzane dzięki {@code @Valid} w rekordach. */
    @PostMapping("/orders")
    public Map<String, Object> placeOrder(@Valid @RequestBody OrderRequest request) {
        return Map.of("customer", request.customer(), "items", request.items().size());
    }

    /** {@code @Validated(OnCreate.class)} → TYLKO reguły z grupy OnCreate. */
    @PostMapping("/products")
    public Map<String, Object> create(@Validated(OnCreate.class) @RequestBody ProductDto product) {
        return Map.of("created", product.name());
    }

    /** {@code @Validated(OnUpdate.class)} → TYLKO reguły z grupy OnUpdate. */
    @PutMapping("/products")
    public Map<String, Object> update(@Validated(OnUpdate.class) @RequestBody ProductDto product) {
        return Map.of("updated", product.name());
    }

    /** Pułapka: zwykłe {@code @Valid} nie uruchamia reguł z grup — ProductDto przechodzi bez żadnej kontroli. */
    @PostMapping("/products-with-valid")
    public Map<String, Object> createWithPlainValid(@Valid @RequestBody ProductDto product) {
        return Map.of("created", String.valueOf(product.name()));
    }

    /** Pułapka Default: grupa OnCreate sama nie obejmuje reguł bez grupy (name). */
    @PostMapping("/mixed-only-oncreate")
    public Map<String, Object> mixedOnlyOnCreate(@Validated(OnCreate.class) @RequestBody MixedDto dto) {
        return Map.of("ok", true);
    }

    /** Naprawa: podajemy obie grupy — Default (reguły bez grupy) i OnCreate. */
    @PostMapping("/mixed-with-default")
    public Map<String, Object> mixedWithDefault(
            @Validated({jakarta.validation.groups.Default.class, OnCreate.class}) @RequestBody MixedDto dto) {
        return Map.of("ok", true);
    }
}
