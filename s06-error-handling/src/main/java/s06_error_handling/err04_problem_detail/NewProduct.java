package s06_error_handling.err04_problem_detail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Dane nowego produktu; własne polskie komunikaty ({@code message = "..."}), niezależne od języka systemu. */
public record NewProduct(
        @NotBlank(message = "Nazwa nie może być pusta") String name,
        @Positive(message = "Cena musi być dodatnia") BigDecimal price) {
}
