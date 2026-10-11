package s05_validation.val02_valid_in_controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * PlainController = kontroler BEZ własnej obsługi błędów — pokazuje, co Spring robi domyślnie (sekcje 2 i 3).
 */
@RestController
@RequestMapping("/plain")
public class PlainController {

    @PostMapping("/products")
    public Map<String, Object> create(@Valid @RequestBody ProductController.ProductRequest request) {
        return Map.of("name", request.name());
    }

    /** BEZ {@code @Valid} — adnotacje w ProductRequest są ignorowane (sekcja 1, pułapka). */
    @PostMapping("/unchecked")
    public Map<String, Object> unchecked(@RequestBody ProductController.ProductRequest request) {
        return Map.of("name", request.name());
    }

    @GetMapping("/items")
    public Map<String, Object> items(@RequestParam @Positive int limit) {
        return Map.of("limit", limit);
    }
}
