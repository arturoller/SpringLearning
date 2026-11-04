package s12_security.sec02_authorization;

/**
 * SZKIELET ĆWICZENIA 1 lekcji Sec02Authorization: zamiana nazwy roli na nazwę uprawnienia — to samo, co robi w środku
 * {@code hasRole("ADMIN")}.
 */
public final class AuthorityNames {

    private AuthorityNames() {
    }

    /**
     * Ćwiczenie 1: {@code "ADMIN"} → {@code "ROLE_ADMIN"}. Nazwa, która JUŻ zaczyna się od {@code ROLE_}, to błąd programisty:
     * rzuć {@code IllegalArgumentException} (tak jak {@code hasRole} w regułach adresów).
     */
    public static String roleToAuthority(String role) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
