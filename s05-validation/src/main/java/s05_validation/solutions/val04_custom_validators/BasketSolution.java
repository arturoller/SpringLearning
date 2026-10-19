package s05_validation.solutions.val04_custom_validators;

/** Rekord z adnotacją z rozwiązania ćwiczenia 3 (odpowiednik Basket). */
public record BasketSolution(String sku, @QuantitySolution int quantity) {
}
