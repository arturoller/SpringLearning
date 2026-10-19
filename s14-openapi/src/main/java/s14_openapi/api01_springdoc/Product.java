package s14_openapi.api01_springdoc;

import java.math.BigDecimal;

/** Produkt sklepu (sku = kod produktu, price = cena w PLN, stock = stan magazynu). Zwykły rekord — bez adnotacji. */
public record Product(String sku, String name, String category, BigDecimal price, int stock) {
}
