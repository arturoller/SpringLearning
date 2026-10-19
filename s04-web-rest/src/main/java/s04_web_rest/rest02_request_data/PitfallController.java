package s04_web_rest.rest02_request_data;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PitfallController = kontroler z CELOWYM błędem (pitfall = pułapka) — do testu i pliku .http. Nie kopiuj go!
 */
@RestController
@RequestMapping("/api/pitfalls")
public class PitfallController {

    /**
     * Celowo błędnie: parametr opcjonalny (required = false) typu PRYMITYWNEGO int. Gdy go brak, Spring ma wstawić
     * null — a do int nie da się. GET /api/pitfalls/primitive → 500 (IllegalStateException). Poprawnie:
     * {@code Integer}, {@code Optional<Integer>} albo {@code defaultValue}.
     */
    @GetMapping("/primitive")
    public String primitive(@RequestParam(required = false) int limit) {
        return "limit = " + limit;
    }

    /**
     * Celowo błędnie: w adresie zmienna {id}, a w adnotacji nazwa "sku". Spring szuka zmiennej ścieżki „sku”, której nie ma.
     * GET /api/pitfalls/products/ELE-001 → 500 (MissingPathVariableException). Poprawnie: {@code @PathVariable("id")}.
     */
    @GetMapping("/products/{id}")
    public String byId(@PathVariable("sku") String id) {
        return "id = " + id;
    }
}
