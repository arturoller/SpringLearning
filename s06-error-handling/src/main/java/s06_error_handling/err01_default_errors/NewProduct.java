package s06_error_handling.err01_default_errors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Dane nowego produktu z ograniczeniami Bean Validation (szczegóły: s05_validation/Val01BeanValidation). */
public record NewProduct(@NotBlank String name, @Positive BigDecimal price) {
}
