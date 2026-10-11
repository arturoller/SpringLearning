package s04_web_rest.rest02_request_data;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ShopExerciseController = kontroler ćwiczeń lekcji Rest02RequestData — SZKIELET. Adresy są gotowe, ale metody NIE MAJĄ
 * parametrów: dopisz je sam (z właściwymi adnotacjami) i uzupełnij ciała. Test: Rest02RequestDataExercisesTest.
 */
@RestController
public class ShopExerciseController {

    /** Greeting = powitanie: {"text":"Cześć, Ala!"}. */
    public record Greeting(String text) {
    }

    /** Listing = lista produktów kategorii: kategoria z adresu, tagi i limit z parametrów. */
    public record Listing(String category, List<String> tags, int limit) {
    }

    /** CartLine = pozycja koszyka: cena w groszach i ilość. */
    public record CartLine(int priceGrosze, int quantity) {
    }

    /** CartRequest = koszyk w ciele zapytania: {"lines":[{"priceGrosze":1000,"quantity":2}]}. */
    public record CartRequest(List<CartLine> lines) {
    }

    /** CartTotal = wynik: suma po rabacie i zastosowany rabat. */
    public record CartTotal(int totalGrosze, int discountPercent) {
    }

    /**
     * ĆWICZENIE 1 (łatwe): {@code GET /api/exercise/greeting?name=Ala} → {"text":"Cześć, Ala!"}, a bez parametru name →
     * {"text":"Cześć, gościu!"}. Podpowiedź: {@code @RequestParam(defaultValue = "gościu") String name}.
     */
    @GetMapping("/api/exercise/greeting")
    public Greeting greeting() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): {@code GET /api/exercise/categories/kawa/products?tag=bio&tag=fair&limit=5} →
     * {"category":"kawa","tags":["bio","fair"],"limit":5}. Bez tagów → pusta lista {@code []}; bez limit → 10;
     * {@code limit=abc} → 400. Ścieżka ma zmienną — dopisz {@code {category}} do adresu w {@code @GetMapping}!
     * Podpowiedź: {@code @PathVariable}, {@code @RequestParam(required = false) List<String> tag},
     * {@code @RequestParam(defaultValue = "10") int limit}.
     */
    @GetMapping("/api/exercise/categories/TODO/products")
    public Listing listing() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): {@code POST /api/exercise/cart/total} z JSON-em CartRequest w ciele i opcjonalnym
     * nagłówkiem X-Discount-Percent (rabat w procentach, domyślnie 0) → CartTotal. Suma = Σ priceGrosze × quantity,
     * potem rabat z zaokrągleniem: {@code (suma * (100 - rabat) + 50) / 100}. Rabat spoza 0..100 albo pusta lista
     * pozycji → 400 Bad Request (rzuć IllegalArgumentException i dopisz metodę z
     * {@code @ExceptionHandler(IllegalArgumentException.class)} zwracającą {@code ResponseEntity.badRequest().build()}).
     * Przykład: [1000 × 2, 999 × 1], rabat 10 → (2999 × 90 + 50) / 100 = 2699.
     * Podpowiedź: {@code @RequestBody CartRequest cart}, {@code @RequestHeader(name = "X-Discount-Percent",
     * defaultValue = "0") int discount}; ExceptionHandler — jak PriceController w s00_start/Start04TestingIntro.
     */
    @PostMapping("/api/exercise/cart/total")
    public CartTotal cartTotal() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
