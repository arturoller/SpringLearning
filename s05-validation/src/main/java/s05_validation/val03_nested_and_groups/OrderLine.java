package s05_validation.val03_nested_and_groups;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Pozycja zamówienia (element listy).
 *
 * @param productName nazwa produktu
 * @param quantity    ilość sztuk, co najmniej 1
 */
public record OrderLine(
        @NotBlank String productName,
        @NotNull @Min(1) Integer quantity) {
}
