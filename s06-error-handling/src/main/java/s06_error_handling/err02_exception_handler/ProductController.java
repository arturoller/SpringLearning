package s06_error_handling.err02_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Kontroler produktów bez własnych handlerów: błędy zamieniają się w statusy dzięki {@code @ResponseStatus}
 * na wyjątkach albo dzięki {@code ResponseStatusException} (wyjątek, który sam niesie kod statusu).
 */
@RestController
public class ProductController {

    private static final Map<String, String> NAMES = Map.of("ELE-001", "Laptop Pro 14", "KSI-001", "Czysty kod");

    /** Znany SKU → 200; nieznany → ProductNotFoundException (@ResponseStatus 404). */
    @GetMapping("/api/products/{sku}")
    public Map<String, String> one(@PathVariable String sku) {
        String name = NAMES.get(sku);
        if (name == null) {
            throw new ProductNotFoundException(sku);
        }
        return Map.of("sku", sku, "name", name);
    }

    /** Rezerwacja zawsze kończy się 409 — {@code ResponseStatusException} niesie status i powód w jednym obiekcie. */
    @GetMapping("/api/products/{sku}/reserve")
    public String reserve(@PathVariable String sku) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Towar " + sku + " jest chwilowo niedostępny");
    }

    /** Wyjątek z {@code @ResponseStatus(value = GONE, reason = ...)} → 410. */
    @GetMapping("/api/products/{sku}/offer")
    public BigDecimal offer(@PathVariable String sku) {
        throw new DiscontinuedProductException(sku);
    }
}
