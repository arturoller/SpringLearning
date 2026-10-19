package s14_openapi.api01_springdoc;

import java.math.BigDecimal;

/** Dane nowego produktu (ciało zapytania POST). Zwykły rekord — bez adnotacji, bez walidacji. */
public record NewProduct(String sku, String name, String category, BigDecimal price, int stock) {
}
