package s05_validation.val04_custom_validators;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** PromotionRequest = promocja obowiązująca od–do (reguła zakresu dat na poziomie całego rekordu). */
@ValidDateRange
public record PromotionRequest(
        @NotBlank String name,
        @NotNull LocalDate from,
        @NotNull LocalDate to) implements HasDateRange {
}
