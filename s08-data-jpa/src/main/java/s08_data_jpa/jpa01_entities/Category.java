package s08_data_jpa.jpa01_entities;

/**
 * Kategoria produktu (enum = typ wyliczeniowy). W encji {@link Product} zapisywana jako TEKST
 * ({@code @Enumerated(EnumType.STRING)}), a nie jako numer pozycji — dlaczego, wyjaśnia sekcja 3 lekcji.
 */
public enum Category {
    ELEKTRONIKA, SPOZYWCZE, KSIAZKI, ODZIEZ, DOM
}
