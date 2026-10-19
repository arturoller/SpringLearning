package s04_web_rest.solutions.rest02_request_data;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import s04_web_rest.rest02_request_data.Product;
import s04_web_rest.rest02_request_data.Rest02ExerciseController.CartLine;
import s04_web_rest.rest02_request_data.Rest02ExerciseController.CartRequest;
import s04_web_rest.rest02_request_data.Rest02ExerciseController.SearchQuery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Rest02RequestData — zajrzyj dopiero po własnej próbie!
 * Rekordy CartLine, CartRequest, SearchQuery i dane produktów (Product.ALL) pochodzą z pakietu lekcji.
 */
@RestController
@RequestMapping("/api/cw02")
public class Rest02SolutionController {

    /** Produkty z lekcji jako mapa sku → produkt (szybkie wyszukiwanie po sku). */
    private static final Map<String, Product> BY_SKU = Product.ALL.stream()
            .collect(Collectors.toMap(Product::sku, Function.identity()));

    /** Rozwiązanie 1: zmienna ścieżki gotowa w parametrze — wystarczy złożyć tekst. */
    @GetMapping("/greeting/{name}")
    public String greeting(@PathVariable String name) {
        return "Dzień dobry, " + name + "!";
    }

    /** Rozwiązanie 2: net wymagany (brak → 400), vat z wartością domyślną (może być int, bo nigdy nie będzie null). */
    @GetMapping("/gross")
    public BigDecimal gross(@RequestParam BigDecimal net, @RequestParam(defaultValue = "23") int vat) {
        return net.multiply(BigDecimal.valueOf(100L + vat))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** Rozwiązanie 3: ciało JSON → rekord; suma cen razy ilość; nieznany sku → 400 przez ResponseStatusException. */
    @PostMapping("/cart-total")
    public BigDecimal cartTotal(@RequestBody CartRequest cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartLine line : cart.lines()) {
            Product product = BY_SKU.get(line.sku());
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznany produkt: " + line.sku());
            }
            total = total.add(product.price().multiply(BigDecimal.valueOf(line.quantity())));
        }
        return total;
    }

    /** Rozwiązanie 4: @ModelAttribute buduje SearchQuery z parametrów; null = warunek pomijany. */
    @GetMapping("/search")
    public List<String> search(@ModelAttribute SearchQuery query) {
        return Product.ALL.stream()
                .filter(product -> query.text() == null || product.name().toLowerCase(Locale.ROOT)
                        .contains(query.text().toLowerCase(Locale.ROOT)))
                .filter(product -> query.category() == null || product.category() == query.category())
                .filter(product -> query.maxPrice() == null || product.price().compareTo(query.maxPrice()) <= 0)
                .map(Product::sku)
                .sorted()
                .toList();
    }
}
