package s06_error_handling.err03_controller_advice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Nowe zamówienie z ograniczeniami Bean Validation (szczegóły: s05_validation/Val01BeanValidation). Komunikaty są
 * własne i polskie ({@code message = "..."}), więc nie zależą od ustawień językowych komputera.
 */
public record NewOrder(
        @NotBlank(message = "Podaj imię i nazwisko klienta") String customer,
        @NotEmpty(message = "Zamówienie musi mieć co najmniej jedną pozycję") List<@Valid OrderLine> lines) {
}
