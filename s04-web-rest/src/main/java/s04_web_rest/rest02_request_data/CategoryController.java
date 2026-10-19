package s04_web_rest.rest02_request_data;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/**
 * CategoryController = produkty według kategorii. Pokazuje zmienne ścieżki typu enum i kilka zmiennych w jednym adresie.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    /** GET /api/categories/KSIAZKI/products — tekst "KSIAZKI" Spring zamienia na Category.KSIAZKI (Enum.valueOf). */
    @GetMapping("/{category}/products")
    public List<Product> products(@PathVariable Category category) {
        return Product.ALL.stream()
                .filter(product -> product.category() == category)
                .sorted(Comparator.comparing(Product::sku))
                .toList();
    }

    /**
     * GET /api/categories/KSIAZKI/products/KSI-002 — dwie zmienne ścieżki. Nazwy podane wprost w adnotacji
     * ({@code @PathVariable("sku")}), bo parametr Javy nazywa się inaczej (code) niż zmienna w adresie ({sku}).
     */
    @GetMapping("/{category}/products/{sku}")
    public Product product(@PathVariable("category") Category category, @PathVariable("sku") String code) {
        return Product.ALL.stream()
                .filter(product -> product.category() == category && product.sku().equals(code))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nie ma produktu " + code + " w kategorii " + category));
    }
}
