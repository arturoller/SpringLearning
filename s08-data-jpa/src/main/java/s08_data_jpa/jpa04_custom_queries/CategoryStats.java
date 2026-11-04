package s08_data_jpa.jpa04_custom_queries;

/**
 * Wynik zapytania z agregatami ({@code count}, {@code avg} + {@code group by}) lekcji Jpa04CustomQueries, sekcja 5.
 * Typy pól = typy, które zwraca JPQL: {@code count(…)} → {@code Long}, {@code avg(…)} → {@code Double}
 * (nawet dla BigDecimal! — sprawdzone testem).
 */
public record CategoryStats(Category category, Long productCount, Double averagePrice) {
}
