package s07_data_jdbc.jdbc05_repository;

import java.math.BigDecimal;

/** Produkt zapisany w bazie (ma już id nadane przez bazę). Cena: złote jako {@code BigDecimal}. */
public record Product(long id, String sku, String name, String category, BigDecimal price, int stock) {
}
