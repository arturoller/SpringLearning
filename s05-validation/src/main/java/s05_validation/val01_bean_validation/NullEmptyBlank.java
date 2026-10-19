package s05_validation.val01_bean_validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * NullEmptyBlank = cztery pola z różnymi ograniczeniami, żeby zobaczyć RÓŻNICE między {@code @NotNull},
 * {@code @NotEmpty}, {@code @NotBlank} i {@code @Size} dla wartości: {@code null}, {@code ""}, {@code " "}, {@code "x"}.
 */
public record NullEmptyBlank(
        @NotNull String notNull,
        @NotEmpty String notEmpty,
        @NotBlank String notBlank,
        @Size(min = 2) String size) {
}
