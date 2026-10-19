package s05_validation.val02_valid_in_controllers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** CustomerRequest = formularz rejestracji klienta (ograniczenia już są) — używany w ćwiczeniu 1 lekcji Val02. */
public record CustomerRequest(
        @NotBlank String name,
        @NotBlank @Email String email) {
}
