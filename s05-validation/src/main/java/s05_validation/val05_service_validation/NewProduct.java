package s05_validation.val05_service_validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** NewProduct = polecenie dodania produktu (command = polecenie, dane wejściowe operacji serwisu). */
public record NewProduct(
        @NotBlank(message = "Nazwa jest wymagana") String name,
        @NotNull @Pattern(regexp = ProductService.SKU_FORMAT, message = "SKU ma format np. ELE-001") String sku,
        @NotNull @Positive(message = "Cena musi być dodatnia") BigDecimal price) {
}
