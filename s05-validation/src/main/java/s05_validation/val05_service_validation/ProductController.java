package s05_validation.val05_service_validation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ProductController = cienka warstwa HTTP nad serwisem. Pokazuje podział pracy: kontroler waliduje kształt zapytania
 * (400 dla klienta), a serwis chroni własne reguły także przed innymi wywołującymi (zadania, kolejki, inne serwisy).
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /** {@code @Valid} w kontrolerze → zły formularz to 400 (MethodArgumentNotValidException), serwis nawet nie ruszy. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody NewProduct command) {
        return service.add(command);
    }

    /**
     * Kontroler NIE sprawdza formatu SKU z adresu — robi to dopiero serwis. Zły SKU → ConstraintViolationException
     * z serwisu, którego MVC domyślnie nie zamienia na 400, więc klient dostaje 500 (domyślnie — obsługa błędów: s06).
     */
    @GetMapping("/{sku}/stock")
    public Map<String, Object> stock(@PathVariable String sku) {
        return Map.of("sku", sku, "stock", service.stockOf(sku));
    }
}
