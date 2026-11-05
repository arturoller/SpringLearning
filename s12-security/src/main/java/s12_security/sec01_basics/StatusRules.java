package s12_security.sec01_basics;

import org.springframework.security.core.Authentication;

/**
 * SZKIELET ĆWICZENIA 4 lekcji Sec01Basics: tabela "401 czy 403?" z sekcji 4 zapisana jako kod.
 */
public final class StatusRules {

    private StatusRules() {
    }

    /**
     * Ćwiczenie 4: jaki status HTTP dostanie klient, gdy adres wymaga roli {@code requiredRole} (bez prefiksu).
     * <ul>
     *   <li>401 — brak uwierzytelnienia ({@code null}), token anonima albo {@code isAuthenticated() == false};</li>
     *   <li>403 — użytkownik znany, ale bez uprawnienia {@code ROLE_<requiredRole>};</li>
     *   <li>200 — użytkownik ma to uprawnienie.</li>
     * </ul>
     */
    public static int statusFor(Authentication authentication, String requiredRole) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
