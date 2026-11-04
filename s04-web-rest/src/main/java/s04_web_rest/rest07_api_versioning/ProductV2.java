package s04_web_rest.rest07_api_versioning;

import java.math.BigDecimal;

/**
 * Kształt produktu w wersji 2.0 API: cena jako obiekt z kwotą i walutą. To zmiana NIEKOMPATYBILNA — klient wersji 1.0,
 * który czyta pole priceGrosze, w tym JSON-ie go nie znajdzie.
 */
public record ProductV2(long id, String name, Price price) {

    /** Cena: kwota (BigDecimal — dokładne ułamki dziesiętne, patrz s04_web_rest/Rest04Json) i kod waluty. */
    public record Price(BigDecimal amount, String currency) {
    }
}
