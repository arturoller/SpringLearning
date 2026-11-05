package s08_data_jpa.jpa04_custom_queries;

import java.math.BigDecimal;

/**
 * Projekcja INTERFEJSOWA (interface projection — rzut na interfejs) lekcji Jpa04CustomQueries, sekcja 3.
 * Same gettery: Spring Data tworzy w locie obiekt-pośrednik (proxy), który zwraca wartości z wyniku zapytania.
 * Nazwa gettera = nazwa pola encji (findBy…) albo alias kolumny w {@code @Query} ({@code select p.name as name}).
 */
public interface ProductSummary {

    String getName();

    BigDecimal getPrice();
}
