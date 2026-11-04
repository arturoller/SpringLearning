package s05_validation.val01_bean_validation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Formularz nowego produktu — rekord z ograniczeniami (constraints) na komponentach. Użyte typy są celowo
 * "opakowane" (Integer, BigDecimal), a nie prymitywne: tylko one mogą mieć wartość null ("brak danych").
 *
 * @param name            nazwa: niepusta, 3–30 znaków
 * @param price           cena: wymagana, co najmniej 0,01, najwyżej 6 cyfr przed i 2 po przecinku
 * @param quantity        ilość: wymagana, od 1 do 1000
 * @param discountPercent zniżka w procentach: opcjonalna (null jest OK), ale jeśli jest — nieujemna
 */
public record ProductForm(
        @NotBlank @Size(min = 3, max = 30) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2) BigDecimal price,
        @NotNull @Min(1) @Max(1000) Integer quantity,
        @PositiveOrZero Integer discountPercent) {
}
