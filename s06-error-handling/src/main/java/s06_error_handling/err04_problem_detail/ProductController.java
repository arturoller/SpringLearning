package s06_error_handling.err04_problem_detail;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Kontroler z błędami różnego rodzaju; żaden nie jest obsłużony w kontrolerze. */
@RestController
public class ProductController {

    private static final Map<String, Integer> STOCK = Map.of("ELE-001", 7, "ELE-002", 0, "KSI-001", 15);

    /** Znany SKU → 200 z liczbą sztuk; nieznany → ProductNotFoundException (zwykły RuntimeException). */
    @GetMapping("/api/products/{sku}")
    public Map<String, Object> one(@PathVariable String sku) {
        Integer stock = STOCK.get(sku);
        if (stock == null) {
            throw new ProductNotFoundException(sku);
        }
        return Map.of("sku", sku, "stock", stock);
    }

    /** Rezerwacja: żądana ilość większa niż stan → OutOfStockException (ErrorResponseException, 409). */
    @PostMapping("/api/products/{sku}/reserve")
    public String reserve(@PathVariable String sku, @RequestParam int quantity) {
        int available = STOCK.getOrDefault(sku, 0);
        if (quantity > available) {
            throw new OutOfStockException(sku, available, quantity);
        }
        return "ZAREZERWOWANO";
    }

    /** Dane z ciała zapytania z walidacją → MethodArgumentNotValidException. */
    @PostMapping("/api/products")
    public NewProduct create(@Valid @RequestBody NewProduct product) {
        return product;
    }

    /** Symulacja błędu wewnętrznego z wrażliwym tekstem. */
    @GetMapping("/api/crash")
    public String crash() {
        throw new IllegalStateException("Awaria bazy: jdbc:h2:mem:sklep, użytkownik sa");
    }

    /** ResponseStatusException (podklasa ErrorResponseException) — z poradą opartą na REEH także staje się problem+json. */
    @GetMapping("/api/products/{sku}/archive")
    public String archive(@PathVariable String sku) {
        throw new ResponseStatusException(HttpStatus.GONE, "Produkt " + sku + " został zarchiwizowany");
    }
}
