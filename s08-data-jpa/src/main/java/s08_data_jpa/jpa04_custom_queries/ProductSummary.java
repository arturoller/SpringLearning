package s08_data_jpa.jpa04_custom_queries;

import java.math.BigDecimal;

/**
 * Projekcja INTERFEJSOWA (projection = rzut, tylko wybrane pola encji). Spring Data tworzy obiekt-pośrednika
 * (proxy), który implementuje ten interfejs: {@code getName()} zwraca kolumnę name, {@code getPrice()} — price.
 * „Zamknięta” (closed): same gettery pól encji, więc zapytanie pobiera tylko te dwie kolumny (sekcja 3 lekcji).
 */
public interface ProductSummary {

    String getName();

    BigDecimal getPrice();
}
