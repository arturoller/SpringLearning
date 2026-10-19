package s05_validation.val04_custom_validators;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** ShopController = produkty i promocje — kontroler używający własnych ograniczeń przez zwykłe {@code @Valid}. */
@RestController
@RequestMapping("/api")
public class ShopController {

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createProduct(@Valid @RequestBody NewProduct product) {
        return Map.of("sku", product.sku(), "category", product.category());
    }

    @PostMapping("/promotions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createPromotion(@Valid @RequestBody PromotionRequest promotion) {
        return Map.of("name", promotion.name());
    }

    @PostMapping("/flash-sales")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createFlashSale(@Valid @RequestBody FlashSale sale) {
        return Map.of("name", sale.name());
    }
}
