package s05_validation.val03_nested_and_groups;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** OrderLine = pozycja zamówienia: SKU produktu i liczba sztuk (1–99). */
public record OrderLine(
        @NotBlank @Pattern(regexp = "[A-Z]{3}-[0-9]{3}", message = "SKU ma format np. ELE-001") String sku,
        @Min(1) @Max(99) int quantity) {
}
