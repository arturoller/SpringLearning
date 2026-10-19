package s14_openapi.api03_groups_and_versions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Publiczne API produktów z wersjonowaniem Spring Framework 7: ten sam adres, różne metody dla różnych wersji
 * (atrybut {@code version} w {@code @GetMapping}; "1.0+" = od wersji 1.0 wzwyż). Szczegóły mechanizmu:
 * s04_web_rest/Rest07ApiVersioning.
 */
@RestController
@RequestMapping("/api/products")
public class VersionedProductController {

    /** Wersja 1 odpowiedzi: cena brutto jako jedna liczba. */
    public record ProductV1(String sku, String name, BigDecimal price) {
    }

    /** Wersja 2 odpowiedzi: cena rozbita na netto i brutto + pole available. */
    public record ProductV2(String sku, String name, PriceV2 price, boolean available) {
    }

    /** Cena w wersji 2. */
    public record PriceV2(BigDecimal net, BigDecimal gross, String currency) {
    }

    private record Item(String name, BigDecimal gross, int stock) {
    }

    private static final Map<String, Item> ITEMS = Map.of(
            "ELE-001", new Item("Laptop Pro 14", new BigDecimal("5499.99"), 7),
            "ELE-002", new Item("Smartfon X", new BigDecimal("2999.00"), 0),
            "KSI-001", new Item("Czysty kod", new BigDecimal("79.00"), 15));

    /** Lista kodów — jedna metoda dla wszystkich wersji. */
    @GetMapping(version = "1.0+")
    public List<String> skus() {
        return ITEMS.keySet().stream().sorted().toList();
    }

    /** Wersje 1.0 do 1.x: cena jako jedna liczba. */
    @GetMapping(path = "/{sku}", version = "1.0+")
    public ProductV1 oneV1(@PathVariable String sku) {
        Item item = find(sku);
        return new ProductV1(sku, item.name(), item.gross());
    }

    /** Od wersji 2.0: nowy kształt odpowiedzi. */
    @GetMapping(path = "/{sku}", version = "2.0+")
    public ProductV2 oneV2(@PathVariable String sku) {
        Item item = find(sku);
        BigDecimal net = item.gross().divide(new BigDecimal("1.23"), 2, RoundingMode.HALF_UP);
        return new ProductV2(sku, item.name(), new PriceV2(net, item.gross(), "PLN"), item.stock() > 0);
    }

    private static Item find(String sku) {
        Item item = ITEMS.get(sku);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma produktu " + sku);
        }
        return item;
    }
}
