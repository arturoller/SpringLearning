package s05_validation.val02_valid_in_controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

/**
 * ProductController = kontroler z różnymi sposobami walidacji danych wejściowych (lekcja Val02ValidInControllers).
 * Nie ma tu bazy — kontroler odpowiada echem, bo uczymy się tego, co dzieje się PRZED wejściem do metody.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    /** Ciało JSON + {@code @Valid}: błąd walidacji → MethodArgumentNotValidException → 400. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody NewProductRequest request) {
        return ResponseEntity.created(URI.create("/api/products/" + request.sku()))
                .body(Map.of("sku", request.sku(), "name", request.name()));
    }

    /** Ograniczenia na parametrze zapytania — wbudowana walidacja metod Spring MVC (bez {@code @Valid}). */
    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return Map.of("limit", limit);
    }

    /** Ograniczenie na zmiennej ze ścieżki. */
    @GetMapping("/{sku}")
    public Map<String, Object> one(@PathVariable @Pattern(regexp = "[A-Z]{3}-[0-9]{3}", message = "SKU ma format np. ELE-001") String sku) {
        return Map.of("sku", sku);
    }

    /** Ciało z {@code @Valid} ORAZ ograniczenie na innym parametrze — jaki wyjątek przy błędzie? (sekcja 4). */
    @PostMapping("/copies")
    public Map<String, Object> copies(@Valid @RequestBody NewProductRequest request,
                                      @RequestParam @Min(1) int copies) {
        return Map.of("sku", request.sku(), "copies", copies);
    }

    /** BŁĄD: ten sam rekord co w {@code create}, ale BEZ {@code @Valid} — ograniczenia w rekordzie nikogo nie obchodzą. */
    @PostMapping("/unchecked")
    public Map<String, Object> unchecked(@RequestBody NewProductRequest request) {
        return Map.of("sku", String.valueOf(request.sku()), "name", String.valueOf(request.name()));
    }

    /**
     * {@code @Valid} + {@code BindingResult} tuż za nim: wyjątek NIE jest rzucany, a błędy trafiają do
     * {@code errors} — kontroler sam decyduje, co z nimi zrobić (tu: odpowiada 400 z listą pole → komunikat).
     */
    @PostMapping("/manual")
    public ResponseEntity<Map<String, String>> manual(@Valid @RequestBody NewProductRequest request,
                                                      BindingResult errors) {
        if (errors.hasErrors()) {
            Map<String, String> fieldErrors = new TreeMap<>();   // posortowane po nazwie pola
            errors.getFieldErrors().forEach(e -> fieldErrors.put(e.getField(), e.getDefaultMessage()));
            return ResponseEntity.badRequest().body(fieldErrors);
        }
        return ResponseEntity.ok(Map.of("sku", request.sku()));
    }

    /** Obiekt z parametrów adresu + {@code @Valid} (wiązanie jak {@code @ModelAttribute}). */
    @GetMapping("/search")
    public Map<String, Object> search(@Valid ProductFilter filter) {
        return Map.of("category", String.valueOf(filter.category()), "page", String.valueOf(filter.page()));
    }
}
