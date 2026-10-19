package s04_web_rest.rest02_request_data;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ćwiczenia lekcji Rest02RequestData — szkielety do uzupełnienia. Testy: Rest02RequestDataExercisesTest
 * (uruchamiasz sam: ▶ obok klasy testu). Adresy /api/cw02/... nie kolidują z lekcją. Dane: Product.ALL.
 */
@RestController
@RequestMapping("/api/cw02")
public class Rest02ExerciseController {

    /** Jedna pozycja koszyka (ćwiczenie 3): sku i liczba sztuk. */
    public record CartLine(String sku, int quantity) {
    }

    /** Koszyk wysyłany w ciele POST (ćwiczenie 3): {"lines":[{"sku":"KSI-001","quantity":2}, ...]}. */
    public record CartRequest(List<CartLine> lines) {
    }

    /** Kryteria wyszukiwania z parametrów zapytania (ćwiczenie 4). Brak parametru = null = „bez tego warunku”. */
    public record SearchQuery(String text, Category category, BigDecimal maxPrice) {
    }

    /**
     * ĆWICZENIE 1 (łatwe): GET /api/cw02/greeting/Ola → tekst "Dzień dobry, Ola!".
     * Mapowanie i {@code @PathVariable} już są — uzupełnij ciało.
     */
    @GetMapping("/greeting/{name}")
    public String greeting(@PathVariable String name) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): {@code GET /api/cw02/gross?net=100.00&vat=8} → cena brutto 108.00
     * (net * (100 + vat) / 100, zaokrąglenie do 2 miejsc RoundingMode.HALF_UP). Parametr vat jest OPCJONALNY —
     * domyślnie 23: GET /api/cw02/gross?net=100.00 → 123.00. Brak net → 400.
     * Uzupełnij ADNOTACJE parametrów (teraz ich nie ma!) i ciało. Podpowiedź: {@code @RequestParam(defaultValue = ...)};
     * liczenie na BigDecimal — t15_numbers/Numbers01BigDecimal. Ciekawostka: bez adnotacji Spring i tak traktuje
     * parametr prostego typu jak OPCJONALNY {@code @RequestParam} — dlatego brak net daje teraz null, a nie 400.
     */
    @GetMapping("/gross")
    public BigDecimal gross(BigDecimal net, int vat) {
        // TODO: twoje rozwiązanie (dodaj adnotacje parametrów)
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): POST /api/cw02/cart-total z koszykiem w ciele → suma (cena z Product.ALL × liczba sztuk).
     * Przykład: [KSI-001 × 2, SPO-002 × 4] → 79.00 × 2 + 7.49 × 4 = 187.96. Nieznany sku → 400 Bad Request
     * (rzuć {@code new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznany produkt: " + sku)}).
     */
    @PostMapping("/cart-total")
    public BigDecimal cartTotal(@RequestBody CartRequest cart) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): {@code GET /api/cw02/search?text=kaw&category=DOM&maxPrice=2000} → lista sku
     * produktów, które spełniają WSZYSTKIE podane warunki, posortowana: text — nazwa zawiera tekst bez względu na
     * wielkość liter, category — dokładnie ta kategoria, maxPrice — cena NIE większa niż podana. Brak parametru =
     * brak warunku. Przykłady: ?text=kaw → [DOM-001, SPO-001]; {@code ?category=KSIAZKI&maxPrice=100} →
     * [KSI-001, KSI-003]; bez parametrów → wszystkie 14 sku; ?category=ksiazki → 400.
     * Metoda nie ma jeszcze MAPOWANIA (test dostaje 404) — dodaj {@code @GetMapping} i adnotację {@code @ModelAttribute}
     * przy parametrze, potem ciało. Podpowiedź: wzór w ProductFilter.matches.
     */
    public List<String> search(SearchQuery query) {
        // TODO: twoje rozwiązanie (dodaj mapowanie i @ModelAttribute)
        throw new UnsupportedOperationException("TODO");
    }
}
