package s04_web_rest.rest07_api_versioning;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SZKIELET ĆWICZEŃ 1–3 lekcji Rest07ApiVersioning — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Rest07ApiVersioningExercisesTest). Mapowania z wersjami są gotowe — przeczytaj je uważnie, a potem napisz ciała metod.
 * Dopóki metody rzucają UnsupportedOperationException("TODO"), testy ćwiczeń nie przechodzą
 * (są wyłączone z domyślnego {@code ./mvnw test}).
 */
@RestController
@RequestMapping("/rest07/cw/customers")
public class ExerciseController {

    /** Klient w wersji 1.0: imię i nazwisko w jednym polu. */
    public record CustomerV1(long id, String fullName) {
    }

    /** Klient w wersji 2.0: imię i nazwisko osobno (zmiana niekompatybilna). */
    public record CustomerV2(long id, String firstName, String lastName) {
    }

    /** Ćwiczenie 1: wersja 1.0 — zwróć CustomerV1(id, "Jan Kowalski"). */
    @GetMapping(path = "/{id}", version = "1.0")
    public CustomerV1 byIdV1(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 2: wersja 2.0 — zwróć CustomerV2(id, "Jan", "Kowalski"). */
    @GetMapping(path = "/{id}", version = "2.0")
    public CustomerV2 byIdV2(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 3: od wersji 1.1 w górę — zwróć listę numerów zamówień klienta: "Z-" + id + "-1" i "Z-" + id + "-2". */
    @GetMapping(path = "/{id}/orders", version = "1.1+")
    public List<String> orders(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
