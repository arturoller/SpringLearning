package s05_validation.val03_nested_and_groups;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Kupon rabatowy — przykład kolejności grup (sekcja 4): najpierw "tanie" sprawdzenia (czy podano), dopiero potem
 * "droższe" (format, zakres). W prawdziwym projekcie "drogie" to np. zapytanie do bazy lub zewnętrznego serwisu.
 *
 * @param code            kod kuponu, np. ABC-1234
 * @param discountPercent zniżka w procentach, najwyżej 50
 */
public record CouponForm(
        @NotBlank(groups = CouponChecks.Basic.class)
        @Pattern(regexp = "[A-Z]{3}-[0-9]{4}", groups = CouponChecks.Advanced.class) String code,
        @NotNull(groups = CouponChecks.Basic.class)
        @Max(value = 50, groups = CouponChecks.Advanced.class) Integer discountPercent) {
}
