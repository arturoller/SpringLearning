package s05_validation.val05_service_validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** ProductCommand = polecenie dodania produktu przekazywane do serwisu (command = polecenie). */
public record ProductCommand(@NotBlank String name, @Positive int priceGrosze) {
}
