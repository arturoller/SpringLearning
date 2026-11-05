package s04_web_rest.rest04_json;

import java.math.BigDecimal;

/** Produkt sklepu: cena jako {@link BigDecimal} (nie double!), kategoria jako enum. */
public record Product(long id, String name, BigDecimal price, Category category) {
}
