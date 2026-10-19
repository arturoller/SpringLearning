package s05_validation.val04_custom_validators;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * NewProduct = nowy produkt z własnymi ograniczeniami. {@code @NotNull} stoi osobno, bo własne walidatory
 * zwracają true dla null (patrz SkuValidator).
 */
public record NewProduct(
        @NotBlank String name,
        @NotNull @Sku String sku,
        @NotNull @KnownCategory String category,
        @ProductCode String altCode) {
}
