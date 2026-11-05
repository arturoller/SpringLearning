package s12_security.sec04_stateless_api;

import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * SZKIELET ĆWICZENIA 2 lekcji Sec04StatelessApi: reguły CORS dla adresów ćwiczeń {@code /sec04/cw/**}.
 */
public final class ExerciseCors {

    private ExerciseCors() {
    }

    /**
     * Ćwiczenie 2: dla wzorca {@code /sec04/cw/**}: pochodzenie {@code http://localhost:3000}, metody GET i POST, nagłówki
     * {@code Authorization} i {@code Content-Type}, czas pamiętania preflight 1800 sekund. Inne adresy — bez reguł.
     */
    public static UrlBasedCorsConfigurationSource source() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
