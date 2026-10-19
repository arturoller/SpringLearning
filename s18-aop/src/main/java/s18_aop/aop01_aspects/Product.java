package s18_aop.aop01_aspects;

import java.math.BigDecimal;

/** Produkt sklepu (rekord — niezmienny nośnik danych). Cena w złotych jako BigDecimal (dokładne kwoty). */
public record Product(String sku, String name, String category, BigDecimal price, int stock) {
}
