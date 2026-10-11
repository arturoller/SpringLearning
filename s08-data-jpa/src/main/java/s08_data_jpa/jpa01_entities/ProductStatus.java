package s08_data_jpa.jpa01_entities;

/**
 * ProductStatus = status produktu w sklepie: ACTIVE (aktywny, w sprzedaży), HIDDEN (ukryty), DISCONTINUED (wycofany).
 * KOLEJNOŚĆ stałych ma znaczenie przy {@code @Enumerated(EnumType.ORDINAL)} — zobacz LegacyProduct i sekcję 4 lekcji.
 */
public enum ProductStatus {
    ACTIVE,
    HIDDEN,
    DISCONTINUED
}
