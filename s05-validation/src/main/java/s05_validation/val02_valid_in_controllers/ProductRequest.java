package s05_validation.val02_valid_in_controllers;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO wejściowe dla POST/PUT produktu, z ograniczeniami (Val01BeanValidation) i WŁASNYMI komunikatami po polsku
 * (nie zależą od Locale, więc testy mogą je porównywać). Typy opakowane (BigDecimal, Integer),
 * żeby {@code @NotNull} miało sens — brakujące pole w JSON daje null.
 *
 * @param name     nazwa: niepusta, najwyżej 50 znaków
 * @param price    cena: wymagana, co najmniej 0,01
 * @param quantity ilość w magazynie: wymagana, nieujemna
 */
public record ProductRequest(
        @NotBlank(message = "Nazwa jest wymagana") @Size(max = 50, message = "Nazwa może mieć najwyżej {max} znaków") String name,
        @NotNull(message = "Cena jest wymagana") @DecimalMin(value = "0.01", message = "Cena musi wynosić co najmniej {value}") BigDecimal price,
        @NotNull(message = "Ilość jest wymagana") @Min(value = 0, message = "Ilość nie może być ujemna") Integer quantity) {
}
