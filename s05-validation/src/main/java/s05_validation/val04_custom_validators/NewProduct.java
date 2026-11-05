package s05_validation.val04_custom_validators;

/** Nowy produkt: kod SKU i kategoria sprawdzana w katalogu. */
public record NewProduct(@Sku String sku, @CategoryExists String category) {
}
