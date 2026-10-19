package s04_web_rest.rest03_responses;

import java.math.BigDecimal;

/** Product = produkt sklepu (rekord — niezmienny). Cena w złotych jako BigDecimal. */
public record Product(String sku, String name, BigDecimal price, int stock) {
}
