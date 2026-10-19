package s05_validation.val04_custom_validators;

import java.math.BigDecimal;

/** PriceFilter = widełki cenowe filtra wyszukiwania — rekord do ćwiczenia 2 lekcji Val04CustomValidators. */
@ValidPriceRange
public record PriceFilter(BigDecimal minPrice, BigDecimal maxPrice) implements HasPriceRange {
}
