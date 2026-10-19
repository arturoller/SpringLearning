package s12_security.sec02_authorization;

import java.util.Map;

/**
 * ĆWICZENIE 4 (trudniejsze) lekcji Sec02Authorization — pensje pracowników.
 *
 * <p>Celowo BEZ {@code @Service}: Sec02Application jej nie skanuje, więc nierozwiązane ćwiczenie nie psuje aplikacji
 * lekcji. Test (Sec02AuthorizationExercisesTest) sam rejestruje tę klasę razem z {@code @EnableMethodSecurity}.</p>
 */
public class ExerciseSalaryService {

    /** Pensje w złotych (dane sklepu). */
    private final Map<String, Integer> salaries = Map.of("anna", 14500, "piotr", 9800, "kasia", 7200, "tomasz", 8900);

    /**
     * Zwróć pensję pracownika z mapy (nieznany pracownik → {@code java.util.NoSuchElementException}).
     * Dopisz nad metodą {@code @PreAuthorize}: pensję KAŻDEGO widzi rola HR, a każdy pracownik — tylko SWOJĄ.
     * Oczekiwane: kasia (HR) → salaryOf("anna") = 14500; piotr → salaryOf("piotr") = 9800;
     * piotr → salaryOf("anna") → AuthorizationDeniedException.
     * Podpowiedź: OrderService.ordersOf — #nazwaParametru, authentication.name, hasRole('...'), „or”.
     */
    public int salaryOf(String employee) {
        // TODO: twoje rozwiązanie (i adnotacja nad metodą)
        throw new UnsupportedOperationException("TODO");
    }
}
