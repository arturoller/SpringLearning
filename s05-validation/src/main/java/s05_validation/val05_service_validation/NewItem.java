package s05_validation.val05_service_validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Pozycja do zarejestrowania w magazynie. Adnotacje działają dopiero, gdy ktoś obiekt zwaliduje (Validator, @Valid). */
public record NewItem(@NotBlank String sku, @Positive int quantity) {
}
