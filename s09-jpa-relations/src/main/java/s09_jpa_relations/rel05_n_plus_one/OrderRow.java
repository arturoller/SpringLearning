package s09_jpa_relations.rel05_n_plus_one;

import java.math.BigDecimal;

/**
 * Wiersz listy zamówień (sekcja 5) — projekcja DTO wypełniana zapytaniem {@code select new …}: numer, klient, liczba
 * pozycji i wartość. JEDNO zapytanie z JOIN i GROUP BY, zero encji w pamięci.
 */
public record OrderRow(String orderNumber, String customerName, long lineCount, BigDecimal total) {
}
