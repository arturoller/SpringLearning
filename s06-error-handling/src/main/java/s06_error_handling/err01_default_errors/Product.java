package s06_error_handling.err01_default_errors;

import java.math.BigDecimal;

/** Produkt sklepu (ceny w PLN jako BigDecimal). */
public record Product(String sku, String name, BigDecimal price) {
}
