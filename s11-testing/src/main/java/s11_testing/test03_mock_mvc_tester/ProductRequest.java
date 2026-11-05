package s11_testing.test03_mock_mvc_tester;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** DTO wejściowe dla POST. Komunikaty własne — dzięki temu testy nie zależą od języka systemu. */
public record ProductRequest(
        @NotBlank(message = "nazwa nie może być pusta") String name,
        @PositiveOrZero(message = "cena nie może być ujemna") BigDecimal price) {
}
