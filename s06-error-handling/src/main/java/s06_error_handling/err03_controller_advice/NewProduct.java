package s06_error_handling.err03_controller_advice;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Dane nowego produktu z ograniczeniami walidacji i polskimi komunikatami. */
public record NewProduct(
        @NotBlank(message = "Nazwa jest wymagana") String name,
        @Min(value = 1, message = "Ilość musi wynosić co najmniej 1") Integer quantity) {
}
