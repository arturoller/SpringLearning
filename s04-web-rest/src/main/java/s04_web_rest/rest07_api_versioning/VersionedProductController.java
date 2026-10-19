package s04_web_rest.rest07_api_versioning;

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
 * VersionedProductController = produkty z WBUDOWANYM wersjonowaniem Spring Framework 7: ten sam adres, różne metody
 * dla różnych wersji API. Wersję wybiera atrybut {@code version} w {@code @GetMapping}:
 * "1.0+" — wersja BAZOWA (baseline): od 1.0 wzwyż, dopóki wyższa wersja nie przejmie adresu;
 * "1.0" — wersja STAŁA: dokładnie 1.0 i nic więcej.
 */
@RestController
@RequestMapping("/api/products")
public class VersionedProductController {

    /** Wersja 1 odpowiedzi: cena brutto jako jedna liczba. */
    public record ProductV1(String sku, String name, BigDecimal price) {
    }

    /** Wersja 2 odpowiedzi: cena rozbita na netto, VAT i brutto + pole available. */
    public record ProductV2(String sku, String name, PriceV2 price, boolean available) {
    }

    /** Cena w wersji 2. */
    public record PriceV2(BigDecimal net, int vatRate, BigDecimal gross, String currency) {
    }

    /** Stan magazynu — punkt końcowy dodany w wersji 1.1. */
    public record Stock(String sku, int stock) {
    }

    private record Item(String name, BigDecimal gross, int stock) {
    }

    private static final Map<String, Item> ITEMS = Map.of(
            "ELE-001", new Item("Laptop Pro 14", new BigDecimal("5499.99"), 7),
            "ELE-002", new Item("Smartfon X", new BigDecimal("2999.00"), 0),
            "KSI-001", new Item("Czysty kod", new BigDecimal("79.00"), 15));

    /** Lista sku — „1.0+”: jedna metoda dla WSZYSTKICH wersji (nic się w niej nie zmieniało). */
    @GetMapping(version = "1.0+")
    public List<String> skus() {
        return ITEMS.keySet().stream().sorted().toList();
    }

    /** GET /api/products/ELE-001 w wersjach 1.0, 1.1, 1.5 (baseline "1.0+" — aż do 2.0). */
    @GetMapping(path = "/{sku}", version = "1.0+")
    public ProductV1 oneV1(@PathVariable String sku) {
        Item item = find(sku);
        return new ProductV1(sku, item.name(), item.gross());
    }

    /** GET /api/products/ELE-001 od wersji 2.0 wzwyż — nowy kształt odpowiedzi. */
    @GetMapping(path = "/{sku}", version = "2.0+")
    public ProductV2 oneV2(@PathVariable String sku) {
        Item item = find(sku);
        BigDecimal net = item.gross().divide(new BigDecimal("1.23"), 2, RoundingMode.HALF_UP);
        return new ProductV2(sku, item.name(), new PriceV2(net, 23, item.gross(), "PLN"), item.stock() > 0);
    }

    /** GET /api/products/ELE-001/stock — dodane w 1.1 ("1.1+"): w wersji 1.0 ten adres NIE istnieje. */
    @GetMapping(path = "/{sku}/stock", version = "1.1+")
    public Stock stock(@PathVariable String sku) {
        return new Stock(sku, find(sku).stock());
    }

    /** GET /api/products/ELE-001/legacy-price — wersja STAŁA "1.0": tylko dla 1.0, każda inna → 400. */
    @GetMapping(path = "/{sku}/legacy-price", version = "1.0")
    public String legacyPrice(@PathVariable String sku) {
        return find(sku).gross() + " PLN";
    }

    private static Item find(String sku) {
        Item item = ITEMS.get(sku);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma produktu " + sku);
        }
        return item;
    }
}
