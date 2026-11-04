package s12_security.solutions.sec01_basics;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Rozwiązanie wzorcowe ćwiczenia 4 lekcji Sec01Basics. */
public final class StatusRules {

    private StatusRules() {
    }

    public static int statusFor(Authentication authentication, String requiredRole) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return 401;                          // nie wiemy, KIM jesteś
        }
        boolean allowed = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(("ROLE_" + requiredRole)::equals);
        return allowed ? 200 : 403;              // wiemy, kim jesteś — ale czy Ci wolno?
    }
}
