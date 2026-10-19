package s05_validation.val05_service_validation;

import java.math.BigDecimal;

/** Product = produkt w pamięci serwisu (sku, nazwa, cena w złotych). */
public record Product(String sku, String name, BigDecimal price) {
}
