package s22_extras.ext05_webflux_overview;

import java.math.BigDecimal;

/** Produkt w lekcji Ext05WebFluxOverview. Cena w złotych (BigDecimal), stan magazynu w sztukach. */
public record ReactiveProduct(String sku, String name, BigDecimal price, int stock) {
}
