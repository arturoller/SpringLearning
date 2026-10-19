package s08_data_jpa.jpa04_custom_queries;

import java.math.BigDecimal;

/**
 * Projekcja interfejsowa dla zapytania z AGREGACJĄ (count, sum, group by) — czegoś, czego nie zbudujesz z nazwy
 * metody. Gettery dopasowują się do ALIASÓW w zapytaniu: {@code count(p) as productCount} → {@code getProductCount()}.
 */
public interface CategoryStats {

    Category getCategory();

    long getProductCount();

    BigDecimal getStockValue();            // suma cena × stan — wartość towaru w magazynie
}
