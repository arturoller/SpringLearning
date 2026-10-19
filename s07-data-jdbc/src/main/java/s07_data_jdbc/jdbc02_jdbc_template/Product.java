package s07_data_jdbc.jdbc02_jdbc_template;

import java.math.BigDecimal;

/** Produkt sklepu na potrzeby lekcji Jdbc02JdbcTemplate (każda lekcja ma własny rekord — lekcje są niezależne). */
public record Product(long id, String sku, String name, String category, BigDecimal price, int stock) {
}
