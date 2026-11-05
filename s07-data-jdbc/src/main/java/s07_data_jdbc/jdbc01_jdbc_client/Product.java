package s07_data_jdbc.jdbc01_jdbc_client;

import java.math.BigDecimal;

/** Produkt ze sklepu — niezmienny rekord; nazwy pól odpowiadają kolumnom (discountPercent ↔ discount_percent). */
public record Product(Long id, String name, String category, BigDecimal price, int stock, Integer discountPercent) {
}
