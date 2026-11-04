package s04_web_rest.rest02_request_data;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SZKIELET ĆWICZEŃ lekcji Rest02RequestData — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA;
 * testy: Rest02RequestDataExercisesTest). Mapowania metod są gotowe. Ty dodajesz adnotacje przy PARAMETRACH
 * (@PathVariable, @RequestParam…) i piszesz treść metod. Dopóki metody rzucają UnsupportedOperationException("TODO"),
 * testy ćwiczeń nie przechodzą (są wyłączone z domyślnego {@code ./mvnw test}).
 */
@RestController
@RequestMapping("/rest02/cw")
public class ExerciseController {

    /** Rekord do ćwiczenia 4 (gotowy). */
    public record Coupon(String code, Integer percent) {
    }

    /** Ćwiczenie 1: zwróć "klient 7, zamówienie 12" dla {@code GET /rest02/cw/customers/7/orders/12}. */
    @GetMapping("/customers/{customerId}/orders/{orderId}")
    public String order(Long customerId, Long orderId) {
        // TODO: dodaj adnotacje do parametrów i zwróć napis
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 2: zwróć "Cześć, Ola!"; gdy parametru name brak — "Cześć, gościu!". */
    @GetMapping("/greet")
    public String greet(String name) {
        // TODO: dodaj adnotację z wartością domyślną i zwróć napis
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 3: zwróć "język: " + wartość nagłówka X-Lang; bez nagłówka ma być 400. */
    @GetMapping("/lang")
    public String lang(String language) {
        // TODO: pobierz nagłówek X-Lang adnotacją @RequestHeader
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 4: z JSON-a {"code":"ZIMA10","percent":10} zwróć "kod ZIMA10: -10%". */
    @PostMapping("/coupon")
    public String coupon(Coupon coupon) {
        // TODO: dodaj adnotację odczytującą ciało zapytania i zwróć napis
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 5: dla {@code ?n=1&n=2&n=3} zwróć sumę jako napis ("6"); zły typ (n=abc) ma dać 400. */
    @GetMapping("/sum")
    public String sum() {
        // TODO: dodaj parametr List<Integer> n z właściwą adnotacją i zsumuj liczby
        throw new UnsupportedOperationException("TODO");
    }
}
