package s07_data_jdbc.jdbc02_jdbc_template;

import java.math.BigDecimal;

/** Produkt ze sklepu (niezmienny rekord). */
public record Product(Long id, String name, String category, BigDecimal price, int stock) {
}
