package s13_rest_client.rc04_resilience;

import java.math.BigDecimal;

/**
 * SupplierPrice = cena produktu u dostawcy (price = cena), w złotych jako {@code BigDecimal}.
 * JSON: {"sku":"ELE-001","price":5499.99}.
 */
public record SupplierPrice(String sku, BigDecimal price) {
}
