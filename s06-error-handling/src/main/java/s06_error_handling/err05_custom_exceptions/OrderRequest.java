package s06_error_handling.err05_custom_exceptions;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Dane zamówienia z walidacją Bean Validation (validation = sprawdzanie poprawności). Komunikaty ustawiamy po polsku
 * w {@code message}, bo domyślne zależą od języka serwera. {@code record} = niezmienna klasa danych.
 */
public record OrderRequest(
        @NotBlank(message = "Podaj nazwę klienta") String customer,
        @Positive(message = "Id produktu musi być dodatnie") long productId,
        @Min(value = 1, message = "Minimalna ilość to 1")
        @Max(value = 100, message = "Maksymalna ilość to 100") int quantity) {
}
