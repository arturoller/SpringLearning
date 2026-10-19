package s05_validation.val02_valid_in_controllers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * NewProductRequest = dane nowego produktu z ciała zapytania (JSON). Ograniczenia są na składowych rekordu,
 * ale same nic nie robią — dopiero {@code @Valid} przy parametrze kontrolera każe je sprawdzić.
 */
public record NewProductRequest(
        @NotBlank(message = "Nazwa jest wymagana") String name,
        @NotNull @Pattern(regexp = "[A-Z]{3}-[0-9]{3}", message = "SKU ma format np. ELE-001") String sku,
        @NotNull @Positive(message = "Cena musi być dodatnia") BigDecimal price) {
}
