package s04_web_rest.rest02_request_data;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * ProductQueryController = kontroler zapytań o produkty. Każda metoda pokazuje inny sposób odczytu danych z zapytania:
 * ze ścieżki ({@code @PathVariable}), z parametrów po „?” ({@code @RequestParam}), z ciała ({@code @RequestBody}),
 * z nagłówków ({@code @RequestHeader}) i z wielu parametrów naraz do jednego obiektu ({@code @ModelAttribute}).
 * Dane tylko do odczytu (Product.ALL), więc kontroler nie ma zmiennego stanu.
 */
@RestController
@RequestMapping("/api/products")
public class ProductQueryController {

    // ---------------------------------------------------------------- 1. @PathVariable

    /** GET /api/products/ELE-001 — {@code {sku}} z adresu trafia do parametru o tej samej nazwie. */
    @GetMapping("/{sku}")
    public Product one(@PathVariable String sku) {
        return Product.ALL.stream()
                .filter(product -> product.sku().equals(sku))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma produktu " + sku));
    }

    // ---------------------------------------------------------------- 2. @RequestParam

    /**
     * {@code GET /api/products?limit=3&offset=2} — stronicowanie (paging). defaultValue (wartość domyślna) sprawia,
     * że parametr jest opcjonalny: GET /api/products = limit 5, offset 0. Tekst "3" Spring zamienia na int sam
     * (konwersja typów). ?limit=abc → 400 (MethodArgumentTypeMismatchException).
     */
    @GetMapping
    public List<Product> page(@RequestParam(defaultValue = "5") int limit,
                              @RequestParam(defaultValue = "0") int offset) {
        return Product.ALL.stream()
                .sorted(Comparator.comparing(Product::sku))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    /** GET /api/products/search?name=kawa — parametr WYMAGANY (required = true to domyślna wartość). Brak → 400. */
    @GetMapping("/search")
    public List<Product> search(@RequestParam String name) {
        String wanted = name.toLowerCase(Locale.ROOT);
        return Product.ALL.stream()
                .filter(product -> product.name().toLowerCase(Locale.ROOT).contains(wanted))
                .sorted(Comparator.comparing(Product::sku))
                .toList();
    }

    /**
     * GET /api/products/cheaper-than?max=100 albo {@code ?max=100&category=KSIAZKI}.
     * {@code Optional<Category>} — parametr opcjonalny bez wartości domyślnej: brak = Optional.empty() (t14_optional).
     */
    @GetMapping("/cheaper-than")
    public List<Product> cheaperThan(@RequestParam BigDecimal max, @RequestParam Optional<Category> category) {
        return Product.ALL.stream()
                .filter(product -> product.price().compareTo(max) < 0)
                .filter(product -> category.map(wanted -> product.category() == wanted).orElse(true))
                .sorted(Comparator.comparing(Product::sku))
                .toList();
    }

    /** {@code GET /api/products/by-sku?sku=ELE-001&sku=KSI-001} — ten sam parametr kilka razy → lista. */
    @GetMapping("/by-sku")
    public List<Product> bySku(@RequestParam List<String> sku) {
        return Product.ALL.stream()
                .filter(product -> sku.contains(product.sku()))
                .sorted(Comparator.comparing(Product::sku))
                .toList();
    }

    // ---------------------------------------------------------------- 4. @RequestBody

    /**
     * POST /api/products/price-check z JSON-em {"sku":"KSI-001","quantity":3} → wycena.
     * {@code @RequestBody} — Jackson zamienia CIAŁO zapytania na rekord PriceCheckRequest (przez konstruktor rekordu).
     */
    @PostMapping("/price-check")
    public PriceCheckResponse priceCheck(@RequestBody PriceCheckRequest request) {
        Product product = one(request.sku());
        BigDecimal total = product.price().multiply(BigDecimal.valueOf(request.quantity()));
        return new PriceCheckResponse(product.sku(), request.quantity(), product.price(), total);
    }

    /** Dane wysyłane w ciele POST /api/products/price-check. */
    public record PriceCheckRequest(String sku, int quantity) {
    }

    /** Odpowiedź wyceny: cena jednostkowa (unit price) i razem (total). */
    public record PriceCheckResponse(String sku, int quantity, BigDecimal unitPrice, BigDecimal total) {
    }

    // ---------------------------------------------------------------- 5. @RequestHeader

    /**
     * GET /api/products/ELE-001/label — etykieta cenowa w języku z nagłówka Accept-Language (np. "en-US,en;q=0.9").
     * Brak nagłówka → defaultValue "pl". Nazwy nagłówków w HTTP nie rozróżniają wielkości liter.
     */
    @GetMapping("/{sku}/label")
    public String label(@PathVariable String sku,
                        @RequestHeader(name = "Accept-Language", defaultValue = "pl") String language) {
        Product product = one(sku);
        return language.startsWith("en")
                ? product.name() + " - PLN " + product.price()
                : product.name() + " — " + product.price() + " zł";
    }

    /** GET /api/products/stock-report z nagłówkiem X-Shop-Id: WAW-01 — nagłówek WYMAGANY; brak → 400. */
    @GetMapping("/stock-report")
    public String stockReport(@RequestHeader("X-Shop-Id") String shopId) {
        long unavailable = Product.ALL.stream().filter(product -> product.stock() == 0).count();
        return "Sklep " + shopId + ": " + Product.ALL.size() + " produktów, niedostępne: " + unavailable;
    }

    // ---------------------------------------------------------------- 7. obiekt z parametrów zapytania

    /**
     * {@code GET /api/products/filter?category=ELEKTRONIKA&maxPrice=1500&inStock=true} — wiele parametrów naraz trafia
     * do JEDNEGO obiektu ProductFilter. {@code @ModelAttribute} (atrybut modelu) — Spring tworzy rekord przez jego
     * konstruktor, dopasowując nazwy parametrów do nazw składowych. Brakujący parametr = null.
     */
    @GetMapping("/filter")
    public List<Product> filter(@ModelAttribute ProductFilter filter) {
        return Product.ALL.stream()
                .filter(filter::matches)
                .sorted(Comparator.comparing(Product::sku))
                .toList();
    }
}
