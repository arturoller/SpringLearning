package s12_security.solutions.sec06_security_testing;

/**
 * Rozwiązanie wzorcowe ćwiczenia 2 lekcji Sec06SecurityTesting. Klucz: CsrfFilter stoi PRZED logowaniem i autoryzacją, więc
 * bez tokenu CSRF zawsze 403 — nawet anonim. Z tokenem: anonim 401, CUSTOMER 403, EMPLOYEE i ADMIN (hierarchia) 200.
 */
public final class PanelRules {

    private PanelRules() {
    }

    public static int expectedStatus(String who, boolean withCsrf) {
        if (!withCsrf) {
            return 403;
        }
        return switch (who) {
            case "anonim" -> 401;
            case "anna" -> 403;
            case "ewa", "admin" -> 200;
            default -> throw new IllegalArgumentException("Nieznany użytkownik: " + who);
        };
    }
}
