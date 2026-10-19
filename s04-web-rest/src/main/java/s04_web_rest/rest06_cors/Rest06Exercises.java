package s04_web_rest.rest06_cors;

import org.springframework.web.cors.CorsConfiguration;

import java.util.Map;

/**
 * Ćwiczenia 2–4 lekcji Rest06Cors — bez serwera. CorsConfiguration (konfiguracja CORS) to obiekt, który Spring
 * buduje POD SPODEM i z {@code @CrossOrigin}, i z {@code addCorsMappings}. Tu tworzysz go sam i sprawdzasz jego
 * metodami (checkOrigin, checkHttpMethod...). Testy: Rest06CorsExercisesTest.
 */
public final class Rest06Exercises {

    private Rest06Exercises() {
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj CorsConfiguration dla sklepu: pochodzenie https://sklep.example, metody GET, POST,
     * PUT, DELETE, nagłówek żądania Content-Type, nagłówek odpowiedzi do odczytu Location, ciasteczka dozwolone
     * (allowCredentials = true), max-age 1800 sekund.
     * Podpowiedź: new CorsConfiguration(), potem setAllowedOrigins(List.of(...)), setAllowedMethods(...),
     * setAllowedHeaders(...), setExposedHeaders(...), setAllowCredentials(true), setMaxAge(1800L).
     */
    public static CorsConfiguration shopConfiguration() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): sklep ma panele pod KAŻDĄ subdomeną: https://admin.sklep.example,
     * https://magazyn.sklep.example... — wszystkie z ciasteczkami (allowCredentials = true), tylko metoda GET.
     * Zbuduj konfigurację, która to przepuści, a wywołanie validateAllowCredentials() NIE rzuci wyjątku.
     * Podpowiedź: "*" w setAllowedOrigins razem z credentials jest zabronione — użyj setAllowedOriginPatterns.
     */
    public static CorsConfiguration subdomainsWithCredentials() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): czy przeglądarka wyśle zapytanie wstępne (preflight)? Zapytanie jest „proste”
     * (simple request — bez preflight), gdy JEDNOCZEŚNIE:
     *   • metoda to GET, HEAD albo POST,
     *   • każdy nagłówek ustawiony przez skrypt jest na liście: Accept, Accept-Language, Content-Language, Content-Type
     *     (nazwy bez rozróżniania wielkości liter),
     *   • Content-Type (jeśli jest) to jeden z: application/x-www-form-urlencoded, multipart/form-data, text/plain
     *     (porównuj sam typ, bez parametrów po średniku, np. "text/plain; charset=UTF-8" też jest prosty).
     * W każdym innym przypadku zwróć true (potrzebny preflight). Przykłady: GET bez nagłówków → false;
     * POST z Content-Type: application/json → true; DELETE → true; GET z nagłówkiem X-Shop-Id → true.
     * Uproszczenie: pomijamy rzadkie zasady (długość wartości nagłówków, znaki specjalne).
     */
    public static boolean needsPreflight(String method, Map<String, String> headers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
