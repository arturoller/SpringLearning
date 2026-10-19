package s07_data_jdbc.jdbc05_repository;

import java.math.BigDecimal;

/**
 * Produkt, który dopiero chcemy zapisać — jeszcze BEZ id (id nada baza). Osobny rekord zamiast „Product z id = 0”
 * sprawia, że kompilator nie pozwoli pomylić produktu zapisanego z niezapisanym.
 */
public record NewProduct(String sku, String name, String category, BigDecimal price, int stock) {
}
