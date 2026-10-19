package s06_error_handling.err03_controller_advice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Pozycja zamówienia: SKU i ilość (dodatnia). */
public record OrderLine(
        @NotBlank(message = "SKU nie może być puste") String sku,
        @Positive(message = "Ilość musi być dodatnia") int quantity) {
}
