package s06_error_handling.err01_default_errors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * SZKIELET ĆWICZEŃ lekcji Err01DefaultErrors — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Err01DefaultErrorsExercisesTest). Mapowania są gotowe. Dopóki metody rzucają UnsupportedOperationException("TODO"),
 * testy ćwiczeń nie przechodzą (są wyłączone z domyślnego {@code ./mvnw test}).
 */
@RestController
@RequestMapping("/err01/cw")
public class ExerciseController {

    /** Ćwiczenie 2: dodaj do tej klasy adnotację {@code @ResponseStatus} (import: org.springframework.web.bind.annotation). */
    public static class NoStockException extends RuntimeException {

        public NoStockException(String sku) {
            super("Brak towaru: " + sku);
        }
    }

    /**
     * Ćwiczenie 1: dla id 1 albo 2 zwróć tekst {@code "Zamówienie #<id>"}. Dla każdego innego id rzuć
     * {@code ResponseStatusException} ze statusem 404 i opisem {@code "Nie ma zamówienia <id>"}.
     */
    @GetMapping("/orders/{id}")
    public String order(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 2: dla sku {@code "kawa"} rzuć {@link NoStockException} (po dodaniu do niej {@code @ResponseStatus}
     * klient ma dostać 409). Dla innych sku zwróć tekst {@code "Jest: <sku>"}.
     */
    @GetMapping("/stock/{sku}")
    public String stock(@PathVariable String sku) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 3: rabat w procentach. Dla wartości od 0 do 100 włącznie zwróć {@code "Rabat: <percent>%"}. Dla każdej innej
     * rzuć {@code ResponseStatusException} ze statusem 400 i opisem {@code "Rabat musi być w przedziale 0-100"}.
     */
    @GetMapping("/discount")
    public String discount(@RequestParam int percent) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 4: zamień domyślne ciało błędu Boota (mapa z kluczami {@code status}, {@code error}, {@code path}, a czasem
     * {@code message}) na jednolinijkowy opis. Format: {@code "<status> <error> (<path>)"}, a gdy w mapie jest niepusty
     * {@code message}, dopisz {@code ": <message>"}. Przykłady: {@code "404 Not Found (/err01/x)"},
     * {@code "409 Conflict (/err01/stock/kawa): Brak towaru: kawa"}.
     */
    public static String summarize(Map<String, Object> errorBody) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 5: audyt wycieków. Zwróć alfabetycznie posortowaną listę tych kluczy z mapy, które NIE powinny trafiać do
     * klienta na produkcji: {@code trace}, {@code exception}, {@code errors}. Klucz {@code message} też liczy się jako
     * wyciek, ale tylko gdy jego wartość zawiera tekst {@code "db-"} albo {@code "SELECT"} (szczegóły techniczne).
     * Pusta lista = odpowiedź bezpieczna.
     */
    public static List<String> leakedKeys(Map<String, Object> errorBody) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
