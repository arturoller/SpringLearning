package s08_data_jpa.jpa01_entities;

/**
 * Poziom klienta. W Customer celowo BEZ {@code @Enumerated} — czyli domyślnie ORDINAL (numer stałej: STANDARD = 0,
 * GOLD = 1). To PUŁAPKA z sekcji 2: dopisanie nowej stałej przed GOLD przesunęłoby znaczenie liczb w bazie.
 */
public enum CustomerLevel {
    STANDARD, GOLD
}
