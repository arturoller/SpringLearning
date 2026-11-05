package s04_web_rest.rest04_json;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Zamówienie: data (bez godziny), moment w czasie (UTC), kwota i status. */
public record Order(long id, LocalDate orderDate, Instant placedAt, BigDecimal total, OrderStatus status) {
}
