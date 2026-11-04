package s05_validation.val04_custom_validators;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Promocja w sklepie. Reguła "początek nie później niż koniec" dotyczy DWÓCH pól naraz, więc żadna adnotacja na jednym polu
 * jej nie wyrazi — potrzebne ograniczenie na całej klasie ({@link ValidDateRange}).
 */
@ValidDateRange
public record Promotion(@NotBlank String name, LocalDate start, LocalDate end) {
}
