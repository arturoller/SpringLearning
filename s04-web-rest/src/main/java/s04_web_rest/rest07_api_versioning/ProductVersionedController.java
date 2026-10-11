package s04_web_rest.rest07_api_versioning;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s04_web_rest.rest07_api_versioning.VersionedModels.Price;
import s04_web_rest.rest07_api_versioning.VersionedModels.ProductPage;
import s04_web_rest.rest07_api_versioning.VersionedModels.ProductV1;
import s04_web_rest.rest07_api_versioning.VersionedModels.ProductV2;

import java.math.BigDecimal;
import java.util.List;

/**
 * ProductVersionedController = produkty w kilku wersjach API — sekcje 3–4 lekcji Rest07ApiVersioning.
 * Atrybut {@code version} adnotacji mapującej (nowy w Spring Framework 7) mówi, dla jakiej wersji jest metoda:
 * "2.0" = dokładnie ta wersja (stała); "1.0+" = ta wersja i nowsze (baseline — wersja bazowa), ale tylko do
 * najbliższej wyższej wersji zadeklarowanej dla tego samego adresu.
 * Obsługiwane wersje (ApiVersioningConfig): 1.0, 1.1, 2.0.
 */
@RestController
@RequestMapping("/api/products")
public class ProductVersionedController {

    static final List<ProductV1> PRODUCTS = List.of(
            new ProductV1(1, "Kawa", 2999),
            new ProductV1(2, "Herbata", 1599));

    /** GET /api/products/1 z wersją 1.0 albo 1.1 → stary kształt z priceGrosze. „1.0+” = baseline. */
    @GetMapping(path = "/{id}", version = "1.0+")
    public ProductV1 oneV1(@PathVariable long id) {
        return find(id);
    }

    /** GET /api/products/1 z wersją 2.0 → nowy kształt z obiektem price (zmiana łamiąca). Wersja stała. */
    @GetMapping(path = "/{id}", version = "2.0")
    public ProductV2 oneV2(@PathVariable long id) {
        ProductV1 p = find(id);
        return new ProductV2(p.id(), p.name(), toPrice(p));
    }

    /** GET /api/products z wersją 1.0 → sama tablica produktów. Wersja stała. */
    @GetMapping(version = "1.0")
    public List<ProductV1> allV1() {
        return PRODUCTS;
    }

    /** GET /api/products z wersją 1.1 i nowszymi (także 2.0) → obiekt strony z polem total. Baseline „1.1+”. */
    @GetMapping(version = "1.1+")
    public ProductPage allSince11() {
        return new ProductPage(PRODUCTS, PRODUCTS.size());
    }

    /** GET /api/products/1/price z wersją 1.0 → sama liczba groszy. Wersja STAŁA — nie obejmuje 1.1! */
    @GetMapping(path = "/{id}/price", version = "1.0")
    public int priceV1(@PathVariable long id) {
        return find(id).priceGrosze();
    }

    /** GET /api/products/1/price z wersją 2.0 → obiekt Price. Dla 1.1 nie ma tu ŻADNEJ metody (pułapka w sekcji 4). */
    @GetMapping(path = "/{id}/price", version = "2.0")
    public Price priceV2(@PathVariable long id) {
        return toPrice(find(id));
    }

    private static Price toPrice(ProductV1 p) {
        return new Price(BigDecimal.valueOf(p.priceGrosze(), 2), "PLN");   // 2999 groszy → 29.99
    }

    private static ProductV1 find(long id) {
        return PRODUCTS.stream().filter(p -> p.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Brak produktu " + id));
    }
}
