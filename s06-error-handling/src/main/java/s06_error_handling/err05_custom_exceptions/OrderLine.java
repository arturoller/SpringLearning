package s06_error_handling.err05_custom_exceptions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Pozycja zamówienia. Format (puste SKU, ilość ≤ 0) sprawdza Bean Validation → 400; sens biznesowy sprawdza serwis → 422. */
public record OrderLine(
        @NotBlank(message = "SKU nie może być puste") String sku,
        @Positive(message = "Ilość musi być dodatnia") int quantity) {
}
