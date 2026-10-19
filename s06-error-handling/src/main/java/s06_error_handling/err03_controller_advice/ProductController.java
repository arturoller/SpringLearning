package s06_error_handling.err03_controller_advice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Kontroler produktów: rzuca wyjątki, ale nic nie obsługuje — robi to porada (GlobalErrorAdvice). */
@RestController
public class ProductController {

    private static final Map<String, String> NAMES = Map.of("ELE-001", "Laptop Pro 14", "KSI-001", "Czysty kod");

    @GetMapping("/api/products/{sku}")
    public Map<String, String> one(@PathVariable String sku) {
        String name = NAMES.get(sku);
        if (name == null) {
            throw new ProductNotFoundException(sku);
        }
        return Map.of("sku", sku, "name", name);
    }

    /** Symulacja błędu wewnętrznego (np. awaria bazy) — wyjątek z danymi, których klient nie powinien zobaczyć. */
    @GetMapping("/api/products/{sku}/stock")
    public int stock(@PathVariable String sku) {
        throw new IllegalStateException("Baza nie odpowiada: jdbc:h2:mem:sklep, użytkownik sa");
    }
}
