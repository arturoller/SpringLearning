package s06_error_handling.err02_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Katalog produktów — pokazuje trzy sposoby zgłaszania błędu HTTP: wyjątek z {@code @ResponseStatus},
 * {@code ResponseStatusException} oraz lokalne metody {@code @ExceptionHandler} (obsługują tylko TEN kontroler).
 */
@RestController
public class CatalogController {

    /** Produkt 999 "nie istnieje" (rzuca wyjątek z {@code @ResponseStatus}), 13 — wycofany, reszta istnieje. */
    @GetMapping("/api/catalog/{id}")
    public Map<String, Object> product(@PathVariable long id) {
        if (id == 999) {
            throw new ProductNotFoundException(id);
        }
        if (id == 13) {
            throw new OutdatedProductException("Produkt 13 został wycofany ze sprzedaży");
        }
        return Map.of("id", id, "name", "Kawa");
    }

    /** {@code ResponseStatusException} — status i powód podajemy w miejscu rzucenia, bez osobnej klasy wyjątku. */
    @GetMapping("/api/catalog/{id}/stock")
    public Map<String, Object> stock(@PathVariable long id) {
        if (id == 5) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Towar chwilowo niedostępny: " + id);
        }
        return Map.of("id", id, "stock", 12);
    }

    /** Procent rabatu poza 0..100 → IllegalArgumentException (obsłużony lokalnie jako 400). */
    @GetMapping("/api/catalog/discount")
    public Map<String, Object> discount(@RequestParam int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Rabat musi być w zakresie 0..100: " + percent);
        }
        return Map.of("percent", percent);
    }

    // ---- metody obsługi wyjątków (działają tylko dla CatalogController) ----

    /** Obsługa IllegalArgumentException i jego podklas: 400 + bezpieczny komunikat. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badArgument(IllegalArgumentException e) {
        return Map.of("error", "Niepoprawne dane: " + e.getMessage());
    }

    /** Lokalna obsługa wygrywa z adnotacją {@code @ResponseStatus(GONE)} na klasie wyjątku: odpowiadamy 404, nie 410. */
    @ExceptionHandler(OutdatedProductException.class)
    public ResponseEntity<Map<String, String>> outdated(OutdatedProductException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
