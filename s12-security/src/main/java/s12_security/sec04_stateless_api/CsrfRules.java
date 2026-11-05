package s12_security.sec04_stateless_api;

/**
 * SZKIELET ĆWICZENIA 1 lekcji Sec04StatelessApi: które metody HTTP sprawdza ochrona CSRF.
 */
public final class CsrfRules {

    private CsrfRules() {
    }

    /**
     * Ćwiczenie 1: {@code true}, jeśli Spring Security wymaga tokenu CSRF dla tej metody HTTP (wielkość liter bez znaczenia).
     * Metody "bezpieczne" (tylko odczyt): GET, HEAD, TRACE, OPTIONS → {@code false}; każda inna (POST, PUT, PATCH, DELETE…)
     * → {@code true}.
     */
    public static boolean requiresCsrf(String httpMethod) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
