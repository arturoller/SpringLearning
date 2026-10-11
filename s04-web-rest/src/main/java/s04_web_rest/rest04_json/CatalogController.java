package s04_web_rest.rest04_json;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * CatalogController = kontroler katalogu — sekcje 2–5 lekcji Rest04Json. Dane są STAŁE (konkretne daty, a nie
 * „teraz”), więc JSON jest za każdym razem identyczny i łatwy do sprawdzenia w teście.
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    static final List<CatalogProduct> PRODUCTS = List.of(
            new CatalogProduct(1, "Kawa", 2999, "Ziarnista, 1 kg", LocalDate.of(2026, 10, 1),
                    Instant.parse("2026-10-01T08:30:00Z"), 1800),
            new CatalogProduct(2, "Herbata", 1599, null, LocalDate.of(2026, 9, 15),
                    Instant.parse("2026-09-15T12:00:00Z"), 900));

    /** GET /api/catalog/1 → JSON z adnotacjami: product_name, bez purchasePriceGrosze, daty jako tekst. */
    @GetMapping("/{id}")
    public CatalogProduct one(@PathVariable long id) {
        return PRODUCTS.stream().filter(p -> p.id() == id).findFirst().orElseThrow();
    }

    /** POST /api/catalog/echo → Jackson czyta JSON do rekordu i od razu zapisuje go z powrotem („echo” = odbicie). */
    @PostMapping("/echo")
    public CatalogProduct echo(@RequestBody CatalogProduct product) {
        return product;
    }
}
