package s03_boot_basics.boot01_auto_configuration;

import java.math.BigDecimal;

/**
 * Produkt sklepu (rekord — niezmienny nośnik danych). Cena w złotych jako {@link BigDecimal} (dokładne kwoty).
 *
 * @param sku   kod produktu, np. ELE-001
 * @param name  nazwa
 * @param price cena w PLN
 */
public record Product(String sku, String name, BigDecimal price) {
}
