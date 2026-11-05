package s12_security.solutions.sec02_authorization;

/** Rozwiązanie wzorcowe ćwiczenia 1 lekcji Sec02Authorization. */
public final class AuthorityNames {

    private static final String PREFIX = "ROLE_";

    private AuthorityNames() {
    }

    public static String roleToAuthority(String role) {
        if (role.startsWith(PREFIX)) {
            throw new IllegalArgumentException(role + " nie powinna zaczynać się od " + PREFIX + " — prefiks dopisuje się sam");
        }
        return PREFIX + role;
    }
}
