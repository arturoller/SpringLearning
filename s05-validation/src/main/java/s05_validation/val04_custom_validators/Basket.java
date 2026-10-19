package s05_validation.val04_custom_validators;

/** Basket = pozycja koszyka (SKU i liczba sztuk) — rekord do ćwiczenia 3 lekcji Val04CustomValidators. */
public record Basket(String sku, @Quantity int quantity) {
}
