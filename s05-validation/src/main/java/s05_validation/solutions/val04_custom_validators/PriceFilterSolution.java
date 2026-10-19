package s05_validation.solutions.val04_custom_validators;

import s05_validation.val04_custom_validators.HasPriceRange;

import java.math.BigDecimal;

/** Rekord z adnotacją klasy z rozwiązania ćwiczenia 2 (odpowiednik PriceFilter). */
@PriceRangeSolution
public record PriceFilterSolution(BigDecimal minPrice, BigDecimal maxPrice) implements HasPriceRange {
}
