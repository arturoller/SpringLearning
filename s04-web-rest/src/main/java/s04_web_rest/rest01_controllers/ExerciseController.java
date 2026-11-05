package s04_web_rest.rest01_controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SZKIELET ĆWICZEŃ lekcji Rest01Controllers — uzupełniasz go sam. Opis zadań jest w lekcji (sekcja ĆWICZENIA),
 * testy: Rest01ControllersExercisesTest. Dopóki metody rzucają {@code UnsupportedOperationException("TODO")},
 * testy ćwiczeń nie przechodzą (są wyłączone z domyślnego {@code ./mvnw test}).
 * <p>
 * Prefiks {@code /rest01/cw} jest już ustawiony na klasie. Mapowania GET są gotowe — Ty zmieniasz treść metod
 * i dodajesz to, czego brakuje (opis przy każdej metodzie).
 */
@RestController
@RequestMapping("/rest01/cw")
public class ExerciseController {

    /** Rekord do ćwiczenia 2 (gotowy). */
    public record ShopInfo(String name, int openDays) {
    }

    /** Ćwiczenie 1: zwróć napis "Witaj w sklepie!" (odpowiedź text/plain). */
    @GetMapping("/greeting")
    public String greeting() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 2: zwróć {@code new ShopInfo("Sklep DP21", 6)} — Spring zrobi z niego JSON. */
    @GetMapping("/shop")
    public ShopInfo shop() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 3: GET zwraca "koszyk"; DODAJ drugą metodę pod tą samą ścieżką /cart dla DELETE → "koszyk opróżniony". */
    @GetMapping("/cart")
    public String cart() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 4: zwróć "id;name"; metoda ma odpowiadać TYLKO na Accept: text/csv (inne Accept → 406).
     * Ogranicz to adnotacją (atrybut {@code produces}), nie instrukcją if.
     */
    @GetMapping("/report")
    public String report() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
