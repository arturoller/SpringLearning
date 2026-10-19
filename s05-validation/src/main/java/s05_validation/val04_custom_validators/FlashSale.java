package s05_validation.val04_custom_validators;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** FlashSale = wyprzedaż błyskawiczna — ten sam zakres dat, ale błąd zgłaszany dla CAŁEGO obiektu (bez pola). */
@ValidDateRange(reportOnField = false)
public record FlashSale(
        @NotBlank String name,
        @NotNull LocalDate from,
        @NotNull LocalDate to) implements HasDateRange {
}
