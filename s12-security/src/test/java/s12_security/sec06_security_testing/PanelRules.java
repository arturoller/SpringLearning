package s12_security.sec06_security_testing;

/**
 * SZKIELET ĆWICZENIA 2 lekcji Sec06SecurityTesting: przewidź status, zanim uruchomisz test. Test porówna Twoje przewidywania
 * z prawdziwą aplikacją dla wszystkich kombinacji.
 */
public final class PanelRules {

    private PanelRules() {
    }

    /**
     * Ćwiczenie 2: jaki status dostanie {@code POST /sec06/panel/price} (łańcuch {@code panelChain}: rola EMPLOYEE, sesja,
     * CSRF włączony, hierarchia ról ADMIN > EMPLOYEE > CUSTOMER)?
     *
     * @param who      "anonim", "anna" (CUSTOMER), "ewa" (EMPLOYEE) albo "admin" (ADMIN)
     * @param withCsrf czy żądanie ma poprawny token CSRF
     */
    public static int expectedStatus(String who, boolean withCsrf) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
