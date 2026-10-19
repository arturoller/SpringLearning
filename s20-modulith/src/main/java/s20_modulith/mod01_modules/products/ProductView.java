package s20_modulith.mod01_modules.products;

import java.math.BigDecimal;

/**
 * Widok produktu, który moduł {@code products} pokazuje innym modułom (część API modułu — leży w pakiecie bazowym
 * modułu). View = widok: niezmienny rekord z tym, czego potrzebuje świat zewnętrzny, bez szczegółów przechowywania.
 *
 * @param sku   kod produktu, np. ELE-001
 * @param name  nazwa
 * @param price cena w PLN
 * @param stock liczba sztuk na stanie
 */
public record ProductView(String sku, String name, BigDecimal price, int stock) {
}
