package s05_validation.val04_custom_validators;

/** Najprostszy użytkownik własnego ograniczenia {@link Sku}. */
public record SkuForm(@Sku String sku) {
}
