package s04_web_rest.rest06_cors;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * SZKIELET ĆWICZEŃ lekcji Rest06Cors — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Rest06CorsExercisesTest). To zwykła klasa (NIE bean Springa) ze statycznymi metodami, więc niedokończone metody
 * nie psują startu aplikacji ani innych testów. Dopóki metody rzucają UnsupportedOperationException("TODO"),
 * testy ćwiczeń nie przechodzą (są wyłączone z domyślnego {@code ./mvnw test}).
 */
public final class ExerciseCors {

    private ExerciseCors() {
    }

    /**
     * Ćwiczenie 1: zwróć ORIGIN (pochodzenie) adresu: schemat + "://" + host, a jeśli adres ma jawny port — także
     * ":" + port. Ścieżkę, parametry i fragment pomiń.
     * Przykłady: "https://sklep.example.com/koszyk?id=1" → "https://sklep.example.com",
     * "http://localhost:3000/app/" → "http://localhost:3000".
     */
    public static String originOf(String url) {
        // TODO: twoje rozwiązanie (podpowiedź: java.net.URI.create(url) — getScheme(), getHost(), getPort())
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 2: czy dwa adresy mają to samo pochodzenie (ten sam schemat, host i port)? */
    public static boolean sameOrigin(String firstUrl, String secondUrl) {
        // TODO: twoje rozwiązanie (podpowiedź: użyj originOf)
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 3: konfiguracja CORS dla frontendu sklepu działającego pod "http://localhost:5173":
     * metody GET i POST, nagłówek Content-Type, preflight pamiętany przez 1800 sekund.
     */
    public static CorsConfiguration shopFrontConfig() {
        // TODO: twoje rozwiązanie (new CorsConfiguration(), addAllowedOrigin, addAllowedMethod, addAllowedHeader, setMaxAge)
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 4: konfiguracja dla WSZYSTKICH poddomen partnerów ("https://*.partner.example.com") z poświadczeniami
     * (allowCredentials = true), metoda GET, a przeglądarka ma móc odczytać nagłówek odpowiedzi X-Total-Count.
     */
    public static CorsConfiguration partnersConfig() {
        // TODO: twoje rozwiązanie (uwaga na pułapkę z sekcji 4: "*" + poświadczenia!)
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 5: źródło konfiguracji CORS, które dla ścieżek "/rest06/cw/**" zwraca shopFrontConfig().
     * Test podłączy je do filtra CorsFilter przed ExerciseController.
     */
    public static CorsConfigurationSource source() {
        // TODO: twoje rozwiązanie (UrlBasedCorsConfigurationSource i registerCorsConfiguration)
        throw new UnsupportedOperationException("TODO");
    }
}
