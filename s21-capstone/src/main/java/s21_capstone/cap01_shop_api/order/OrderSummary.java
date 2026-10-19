package s21_capstone.cap01_shop_api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Wiersz LISTY zamówień — projekcja DTO wypełniana prosto z zapytania JPQL ({@code select new ...}), bez encji
 * (s09_jpa_relations/Rel05NPlusOne). Jedno zapytanie z {@code join} i {@code count} daje wszystko, czego potrzebuje
 * lista: nie ma leniwych relacji, więc nie ma N+1.
 *
 * @param lineCount liczba pozycji zamówienia (z {@code count(l)})
 */
public record OrderSummary(Long id, String number, String customerName, OrderStatus status, BigDecimal total,
                           LocalDateTime createdAt, long lineCount) {

    /** Konstruktor dla zapytania JPQL: numer wyliczamy z id, {@code count} zwraca {@code Long}. */
    public OrderSummary(Long id, String customerName, OrderStatus status, BigDecimal total, LocalDateTime createdAt,
                        Long lineCount) {
        this(id, ShopOrder.numberOf(id), customerName, status, total, createdAt, lineCount);
    }
}
