package s05_validation.val01_bean_validation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * ProductForm = formularz nowego produktu (dane, które przychodzą z zewnątrz). Ograniczenia (constraints) stoją na
 * składowych rekordu — Hibernate Validator odczytuje je z adnotacji, więc reguły leżą TUŻ przy danych.
 * Ceny w tej lekcji: {@code BigDecimal} w złotych.
 */
public record ProductForm(
        @NotBlank(message = "Nazwa produktu jest wymagana")
        String name,

        @Pattern(regexp = "[A-Z]{3}-[0-9]{3}", message = "SKU ma format np. ELE-001")
        String sku,

        @NotNull(message = "{val01.price.required}")   // komunikat z pliku ValidationMessages.properties
        @DecimalMin(value = "0.01", message = "Cena musi wynosić co najmniej {value} zł")
        BigDecimal price,

        @Min(0) @Max(1000)
        Integer stock,

        @Size(min = 3, max = 20, message = "Opis: od {min} do {max} znaków")
        String description) {
}
