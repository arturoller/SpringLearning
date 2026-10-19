package s04_web_rest.rest07_api_versioning;

import org.springframework.web.accept.ApiVersionDeprecationHandler;

/**
 * Ćwiczenia 3–4 lekcji Rest07ApiVersioning — bez serwera, na klasach Springa, które działają pod spodem
 * wersjonowania. Testy: Rest07ApiVersioningExercisesTest.
 */
public final class Rest07Exercises {

    private Rest07Exercises() {
    }

    /**
     * ĆWICZENIE 3 (średnie): porównaj dwie wersje tak, jak robi to Spring: wynik -1 (a starsza), 0 (równe), 1 (a nowsza).
     * Przykłady: ("1", "1.0.0") → 0; ("1.2", "1.10") → -1 (porównujemy LICZBY: 10 jest większe od 2, choć jako
     * tekst "1.10" byłoby przed "1.2"); ("v2", "1.9") → 1. Podpowiedź: {@code new SemanticApiVersionParser().parseVersion(tekst)} zwraca obiekt
     * Comparable; wynik compareTo zamień na -1/0/1 przez {@code Integer.signum}.
     */
    public static int compareVersions(String a, String b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): plan wycofania wersji 1.1 — zwróć handler, który dla zapytań w wersji 1.1 doda
     * nagłówki: Deprecation (data 2026-12-01T00:00Z), Link z adresem https://sklep.example/api/zmiany-1-1 oraz
     * Sunset (data 2027-06-30T00:00Z). Inne wersje — bez nagłówków.
     * Podpowiedź: wzór w ApiDeprecationConfig (StandardApiVersionDeprecationHandler, configureVersion(...),
     * ZonedDateTime.of(..., ZoneOffset.UTC)).
     */
    public static ApiVersionDeprecationHandler sunsetPlan() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
