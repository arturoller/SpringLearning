package s11_testing.test06_testcontainers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** DTO wejściowe dla POST: nazwa niepusta, cena nieujemna. */
public record ProductRequest(@NotBlank String name, @PositiveOrZero BigDecimal price) {
}
