package s05_validation.val04_custom_validators;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** CatalogController = kontroler katalogu: przyjmuje produkty i promocje, sprawdzone własnymi ograniczeniami. */
@RestController
@RequestMapping("/api")
public class CatalogController {

    @PostMapping("/catalog-products")
    public Map<String, Object> add(@Valid @RequestBody Val04Types.CatalogProduct product) {
        return Map.of("code", product.code(), "category", String.valueOf(product.category()));
    }

    @PostMapping("/promotions")
    public Map<String, Object> promote(@Valid @RequestBody Val04Types.Promotion promotion) {
        return Map.of("priceGrosze", promotion.priceGrosze(), "discountGrosze", promotion.discountGrosze());
    }
}
