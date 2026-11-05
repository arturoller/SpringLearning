package s12_security.sec02_authorization;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Katalog produktów lekcji Sec02Authorization. Kontroler nie ma ani jednej adnotacji bezpieczeństwa — kto może wywołać którą
 * metodę HTTP, ustalają reguły adresów w {@link Sec02SecurityConfig} (sekcja 1). Dane są stałe, żeby testy nie zależały od
 * kolejności: POST i DELETE tylko odpowiadają, niczego nie zapisują.
 */
@RestController
@RequestMapping("/sec02/products")
public class ProductController {

    /** Dane nowego produktu z ciała żądania. */
    public record NewProduct(String name, BigDecimal price) {
    }

    private static final List<Product> CATALOG = List.of(
            new Product(1, "Kawa ziarnista", new BigDecimal("39.90")),
            new Product(2, "Herbata zielona", new BigDecimal("18.50")));

    @GetMapping
    public List<Product> all() {
        return CATALOG;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody NewProduct request) {
        return new Product(3, request.name(), request.price());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        // usunięcie "na niby" — w tej lekcji liczy się tylko, KTO może tu wejść
    }
}
