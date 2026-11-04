package s12_security.solutions.sec06_security_testing;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Supplier;

/** Rozwiązanie wzorcowe ćwiczenia 4 lekcji Sec06SecurityTesting. */
public final class ExerciseSecurityContext {

    private ExerciseSecurityContext() {
    }

    public static <T> T runAs(String username, String role, Supplier<T> action) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();     // NOWY kontekst — nie zmieniaj cudzego
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                username, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
        SecurityContextHolder.setContext(context);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previous);                            // także po wyjątku
        }
    }
}
