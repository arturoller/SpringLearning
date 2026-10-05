package s00_start.start04_testing_intro;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** PriceController = kontroler cen. {@code GET /api/price?net=10000&vat=23} → {"net":10000,"vat":23,"gross":12300}. */
@RestController
public class PriceController {

    /** PriceResponse = odpowiedź z ceną. Rekord → JSON. */
    public record PriceResponse(int net, int vat, int gross) {
    }

    private final PriceCalculator calculator;

    public PriceController(PriceCalculator calculator) {
        this.calculator = calculator;
    }

    @GetMapping("/api/price")
    public PriceResponse price(@RequestParam int net, @RequestParam int vat) {
        return new PriceResponse(net, vat, calculator.gross(net, vat));
    }

    /**
     * {@code @ExceptionHandler} (obsługa wyjątku) — gdy metoda kontrolera rzuci IllegalArgumentException, zamiast błędu
     * 500 odeślij 400 Bad Request (złe zapytanie) z komunikatem. Pełna obsługa błędów — w dziale s06_error_handling.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
