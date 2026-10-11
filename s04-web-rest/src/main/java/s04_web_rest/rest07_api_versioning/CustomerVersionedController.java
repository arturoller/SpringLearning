package s04_web_rest.rest07_api_versioning;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * CustomerVersionedController = klienci w dwóch wersjach — SZKIELET ćwiczenia 2 lekcji Rest07ApiVersioning.
 * Wersja 1.0 jest gotowa. Test: Rest07ApiVersioningExercisesTest.
 */
@RestController
public class CustomerVersionedController {

    /** CustomerV1 = klient w wersji 1.0: jedno pole z imieniem i nazwiskiem. */
    public record CustomerV1(long id, String name) {
    }

    /** CustomerV2 = klient w wersji 2.0: imię i nazwisko osobno (zmiana łamiąca). */
    public record CustomerV2(long id, String firstName, String lastName) {
    }

    /** Gotowe: {@code GET /api/customers/1} z nagłówkiem API-Version: 1.0 → {"id":1,"name":"Anna Nowak"}. */
    @GetMapping(path = "/api/customers/{id}", version = "1.0")
    public CustomerV1 oneV1(@PathVariable long id) {
        return new CustomerV1(id, "Anna Nowak");
    }

    /**
     * ĆWICZENIE 2 (średnie): ta metoda nie ma jeszcze adnotacji. Dopisz mapowanie tak, żeby
     * {@code GET /api/customers/1} z nagłówkiem API-Version: 2.0 (albo 2) zwracało
     * {"id":1,"firstName":"Anna","lastName":"Nowak"}, a wersja 1.0 dalej trafiała do oneV1.
     * Uzupełnij też ciało (imię i nazwisko — rozdziel "Anna Nowak" po spacji albo wpisz wprost).
     * Podpowiedź: {@code @GetMapping(path = "...", version = "2.0")} i {@code @PathVariable}.
     */
    public CustomerV2 oneV2(long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
