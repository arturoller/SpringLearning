package s04_web_rest.rest07_api_versioning;

import org.springframework.web.accept.StandardApiVersionDeprecationHandler;

/**
 * SZKIELET ĆWICZEŃ 4–5 lekcji Rest07ApiVersioning — zwykła klasa (NIE bean Springa) ze statycznymi metodami,
 * więc niedokończone metody nie psują startu aplikacji ani innych testów.
 */
public final class ExerciseVersions {

    private ExerciseVersions() {
    }

    /**
     * Ćwiczenie 4: czy wersja {@code candidate} jest NOWSZA niż {@code current}? Porównuj jak Spring (wersje
     * semantyczne), a nie jak napisy: "1.10" jest nowsza niż "1.9", "2" to to samo co "2.0.0", a "v2" to "2.0.0".
     */
    public static boolean isNewer(String candidate, String current) {
        // TODO: twoje rozwiązanie (podpowiedź: new SemanticApiVersionParser().parseVersion(...) i compareTo)
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 5: obsługa wycofania wersji 1.1 — data wycofania 2026-06-01T00:00Z, data wyłączenia (sunset)
     * 2027-06-30T00:00Z, link do przewodnika "https://sklep.example.com/docs/api/v2".
     */
    public static StandardApiVersionDeprecationHandler deprecationOf1dot1() {
        // TODO: twoje rozwiązanie (wzór: ApiDeprecationConfig w tej lekcji)
        throw new UnsupportedOperationException("TODO");
    }
}
