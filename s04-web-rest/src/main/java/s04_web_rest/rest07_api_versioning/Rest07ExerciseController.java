package s04_web_rest.rest07_api_versioning;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ćwiczenia 1–2 lekcji Rest07ApiVersioning (3–4 — w klasie Rest07Exercises). Testy: Rest07ApiVersioningExercisesTest
 * (uruchamiasz sam: ▶ obok klasy testu). Wersjonowanie jest już skonfigurowane profilem rest07: nagłówek API-Version
 * albo ?version=, domyślnie 1.0.
 */
@RestController
@RequestMapping("/api/cw07/customers")
public class Rest07ExerciseController {

    /** Klient w wersji 1 API: imię i nazwisko razem. */
    public record CustomerV1(long id, String name) {
    }

    /** Klient w wersji 2 API: imię i nazwisko osobno + miasto. */
    public record CustomerV2(long id, String firstName, String lastName, String city) {
    }

    /**
     * ĆWICZENIE 1 (łatwe): GET /api/cw07/customers/1 w wersjach 1.x → CustomerV1(1, "Jan Kowalski"). Mapowanie z wersją
     * bazową "1.0+" już jest — uzupełnij ciało. Dane: klient 1 = Jan Kowalski z Warszawy, klient 4 = Zofia Krawczyk
     * z Warszawy; inny id → rzuć {@code new ResponseStatusException(HttpStatus.NOT_FOUND)}.
     */
    @GetMapping(path = "/{id}", version = "1.0+")
    public CustomerV1 oneV1(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): od wersji 2.0 ten sam adres ma zwracać CustomerV2(1, "Jan", "Kowalski", "Warszawa").
     * Metoda nie ma jeszcze MAPOWANIA — dodaj {@code @GetMapping} z tym samym adresem i właściwą wersją bazową, potem
     * ciało. Wersje 1.0 i 1.5 mają nadal dostawać CustomerV1, a 2.0 — CustomerV2 (także przez ?version=2.0).
     */
    public CustomerV2 oneV2(@PathVariable long id) {
        // TODO: twoje rozwiązanie (dodaj mapowanie z version)
        throw new UnsupportedOperationException("TODO");
    }
}
