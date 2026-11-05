package s11_testing.test01_test_slices;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** DTO wejściowe dla POST: nazwa niepusta, cena nieujemna (walidacja: s05_validation/Val02ValidInControllers). */
public record ProductRequest(@NotBlank String name, @PositiveOrZero BigDecimal price) {
}
