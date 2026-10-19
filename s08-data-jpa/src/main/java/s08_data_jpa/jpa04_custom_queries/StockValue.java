package s08_data_jpa.jpa04_custom_queries;

import java.math.BigDecimal;

/** Wartość towaru w magazynie dla jednego produktu (cena × stan) — rekord wyniku ćwiczenia 2 lekcji Jpa04CustomQueries. */
public record StockValue(String sku, BigDecimal value) {
}
